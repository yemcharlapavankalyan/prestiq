# Prestiq Eligibility Service

Deterministic scholarship eligibility rules engine and evaluation service for the Prestiq platform.

## Overview

The **Eligibility Service** is the authoritative domain owner of:
1. Versioned scholarship eligibility rule sets (`scholarship_rule_sets`)
2. Structured, allowlisted criteria specifications (`eligibility_criteria`)
3. Pure deterministic rule evaluations (zero-LLM, strictly reproducible)
4. Persisted evaluation results and audit breakdowns (`eligibility_evaluations`)

It does **not** store student profiles or scholarship catalog records; it interacts with `student-service` and `scholarship-service` solely through documented API contracts and inter-service channels.

---

## Technical Stack

- **Java Version:** Java 21 LTS
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Maven with Maven Wrapper (`./mvnw`)
- **Port:** `8084`
- **Database:** PostgreSQL (`prestiq_eligibility_db`)
- **Schema Management:** Flyway
- **Security:** Spring Security + JJWT (0.12.6) + Internal Service Key Validation
- **Documentation:** OpenAPI 3.0 (Springdoc 2.6.0)

---

## Rule Schema & Versioning Model

### 1. `scholarship_rule_sets`
- `id`: BIGSERIAL (Primary Key)
- `scholarship_id`: BIGINT (Logical reference to `scholarship-service`)
- `version`: INT (Unique per scholarship: `CONSTRAINT uq_rule_version UNIQUE(scholarship_id, version)`)
- `status`: VARCHAR(30) (`DRAFT`, `ACTIVE`, `SUPERSEDED`)
- `created_by_user_id`: UUID
- `created_at`: TIMESTAMPTZ

### 2. `eligibility_criteria`
- `id`: BIGSERIAL (Primary Key)
- `rule_set_id`: BIGINT (Foreign key to `scholarship_rule_sets(id)` ON DELETE CASCADE)
- `criterion_code`: VARCHAR(50) (e.g. `MIN_GPA`, `CITIZENSHIP`, `MAX_INCOME`)
- `target_field`: VARCHAR(100) (Allowlisted student attribute path)
- `operator`: VARCHAR(30) (`GTE`, `LTE`, `GT`, `LT`, `EQ`, `BETWEEN`, `IN`, `NOT_IN`, `CONTAINS_ANY`, `IS_TRUE`, `IS_FALSE`, `EQUALS`, `NOT_EQUALS`)
- `target_value`: TEXT / JSON (Scalar, JSON array, or JSON range/threshold object)
- `is_mandatory`: BOOLEAN (Defaults to `true`)
- `weight`: INT (Used for bonus ranking of optional criteria)
- `display_name`: VARCHAR(255)
- `pass_explanation_template`: VARCHAR(500) (Optional custom template with `{studentValue}` and `{targetValue}`)
- `fail_explanation_template`: VARCHAR(500)

### 3. `eligibility_evaluations`
- `id`: UUID (Primary Key)
- `student_id`: UUID (Student's user ID)
- `scholarship_id`: BIGINT
- `rule_set_version`: INT
- `verdict`: VARCHAR(30) (`ELIGIBLE`, `INELIGIBLE`, `NEEDS_VERIFICATION`)
- `match_percentage`: NUMERIC(5,2)
- `criteria_breakdown`: TEXT / JSON (Complete criterion-level status, values, and explanations)
- `evaluated_at`: TIMESTAMPTZ

---

## Evaluation Verdicts & Precedence

Every evaluation produces one of three definitive verdicts:

| Verdict | Condition | Precedence Rule |
| :--- | :--- | :--- |
| **`INELIGIBLE`** | One or more mandatory criteria evaluated to `FAIL`. | **Highest Precedence.** Takes absolute precedence over missing data. A missing GPA cannot overturn an ineligibility due to citizenship or residency restrictions. |
| **`NEEDS_VERIFICATION`** | No mandatory criteria failed, but at least one mandatory criterion is `MISSING_DATA`. | Highlights exact missing fields and provides explicit remediation actions for the student. |
| **`ELIGIBLE`** | All mandatory criteria evaluated to `PASS`. | 100% of mandatory criteria satisfied; optional criteria contribute weighted bonuses up to 100.00%. |

---

## Supported Rule Types & Evaluators

The engine implements a safe, allowlisted Composite AST pattern:
1. **Numeric Threshold & Range:**
   - Evaluates GPA with automatic scale normalization:
     $$\text{Normalized GPA} = \left(\frac{\text{studentGPA}}{\text{gpaScale}}\right) \times \text{targetScale}$$
   - Evaluates family income and credit hours (`GTE`, `LTE`, `GT`, `LT`, `EQ`, `BETWEEN`).
2. **Set Membership:**
   - Case-insensitive, trimmed set containment for `citizenshipCountry`, `stateProvince`, `educationLevel`, and `majorFieldOfStudy` (`IN`, `NOT_IN`, `CONTAINS_ANY`).
3. **Boolean Attributes:**
   - Evaluates `firstGenStudent`, `militaryVeteran`, and `disabilityStatus` (`IS_TRUE`, `IS_FALSE`, `EQ`).
4. **String Matching:**
   - Case-insensitive exact match for `institutionName`, `majorFieldOfStudy` (`EQUALS`, `NOT_EQUALS`).

> **Security Note:** Arbitrary JavaScript, SpEL, SQL, or user-supplied executable code is strictly prohibited and rejected by the evaluator registry.

---

## API Endpoints

### Student Endpoints (`/api/v1/eligibility`)
- `GET /api/v1/eligibility/check/{scholarshipId}`: Evaluates authenticated student against the active scholarship rule set.
- `POST /api/v1/eligibility/evaluate`: Evaluates student with optional simulation profile data.
- `GET /api/v1/eligibility/evaluations/{id}`: Retrieves persisted evaluation result (enforces cross-user isolation).
- `GET /api/v1/eligibility/my-evaluations`: Retrieves evaluation history for authenticated student.

### Administrative Endpoints (`/api/v1/admin/eligibility`)
- `POST /api/v1/admin/eligibility/rulesets`: Create a new versioned rule set.
- `PUT /api/v1/admin/eligibility/rulesets/{id}/activate`: Activate rule set (supersedes existing active versions).
- `GET /api/v1/admin/eligibility/rulesets/{id}`: Get rule set by ID.
- `GET /api/v1/admin/eligibility/rulesets/scholarship/{scholarshipId}`: List all rule set versions for a scholarship.
- `GET /api/v1/admin/eligibility/rulesets/scholarship/{scholarshipId}/active`: Get active rule set.

### Internal Inter-Service Endpoints (`/internal/v1/eligibility`)
- `POST /internal/v1/eligibility/batch-evaluate`: High-speed batch evaluation for `recommendation-service` (requires `X-Internal-Service-Key`).

---

## Local Development & Testing

```bash
# Run unit and integration tests
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean test

# Build executable fat JAR
./mvnw clean package

# Run service locally on port 8084
java -jar target/eligibility-service-0.0.1-SNAPSHOT.jar
```
