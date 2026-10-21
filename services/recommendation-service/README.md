# Prestiq Recommendation Service

Personalized scholarship recommendation, ranking, and student preference engine for the Prestiq platform.

## Overview

The **Recommendation Service** is the authoritative owner of:
1. Student personalized scholarship recommendations and ranking (`student_recommendations`)
2. Explainable recommendation scoring based on deterministic eligibility verdicts, academic affinities, deadline urgencies, and award factors
3. Configurable student recommendation preferences (`student_preferences`)
4. Recommendation lifecycle management (filtering, pagination, dismiss/undismiss, on-demand recalculation)

It adheres strictly to platform security and architectural boundaries:
- **No LLM-Based Decisions:** Recommendation scores are purely mathematical and deterministic.
- **Strict Eligibility Gate:** Any scholarship evaluated as `INELIGIBLE` or with a missing evaluation is **strictly excluded** from recommendation rankings. Ranking scores **never** override deterministic ineligibility.
- **Explainability:** Every recommendation persists a complete factor breakdown (`score_breakdown` JSON) and a human-readable explanation justifying the match.
- **Decoupled Architecture:** No cross-database foreign keys. References `student-service` by `student_id` UUID and `scholarship-service` by `scholarship_id` BIGINT.

---

## Technical Stack

- **Java Version:** Java 21 LTS
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Maven with Maven Wrapper (`./mvnw`)
- **Port:** `8085`
- **Database:** PostgreSQL (`prestiq_recommendation_db`)
- **Schema Management:** Flyway
- **Security:** Spring Security + JJWT (0.12.6) + Internal Service Key Validation
- **Memory Footprint:** Bounded JVM heap (`-Xms64m -Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1`) optimized for an 8 GB MacBook Air M2
- **Documentation:** OpenAPI 3.0 (Springdoc 2.6.0)

---

## Ranking Factors & Weighted Scoring Formula

Composite match scores are computed using the platform-approved weights documented in `docs/architecture/eligibility-engine.md`:

$$\text{Composite Score} = (F_{\text{eligibility}} \times 0.50) + (F_{\text{academic}} \times 0.20) + (F_{\text{urgency}} \times 0.15) + (F_{\text{award}} \times 0.15)$$

### Factor Breakdown

1. **Eligibility Score ($F_{\text{eligibility}}$, 50% Weight):**
   - Derived directly from `eligibility-service`'s deterministic evaluation.
   - `ELIGIBLE`: evaluation `matchPercentage` (or 100.00% if omitted).
   - `NEEDS_VERIFICATION`: evaluation `matchPercentage` (or 50.00% if omitted).
   - `INELIGIBLE` or missing evaluation: **0.00%** (strictly excluded by the Eligibility Gate).

2. **Academic Affinity Score ($F_{\text{academic}}$, 20% Weight):**
   - Base score: 50.0.
   - Education level match: +20 (or +15 if scholarship open to all levels).
   - Major alignment: +30 if student major appears in title/provider.
   - Preference match: +25 if student preferred field aligns with scholarship metadata.
   - Clamped to $[0, 100]$.

3. **Deadline Urgency Score ($F_{\text{urgency}}$, 15% Weight):**
   - Expired ($< 0$ days remaining): 0.00 (excluded).
   - Urgent ($\le 14$ days remaining): 100.00.
   - Approaching ($15 - 30$ days remaining): 85.00.
   - Upcoming ($31 - 60$ days remaining): 70.00.
   - Within 90 days ($61 - 90$ days remaining): 50.00.
   - Distant ($> 90$ days remaining): 30.00.
   - No deadline specified: 50.00.

4. **Award Factor Score ($F_{\text{award}}$, 15% Weight):**
   - If student has `minAwardPreference`: 100.00 if `awardMax >= minAwardPreference`; otherwise scaled proportionally.
   - Without preference: Tiered benchmarks ($\ge \$10,000$: 100.00; $\ge \$5,000$: 80.00; $\ge \$2,000$: 60.00; $\ge \$500$: 40.00; $< \$500$: 30.00).

---

## Database Schema (`prestiq_recommendation_db`)

### `student_recommendations` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PRIMARY KEY | Unique recommendation identifier |
| `student_id` | `UUID` | NOT NULL | Student's user ID |
| `scholarship_id` | `BIGINT` | NOT NULL | Scholarship opportunity ID |
| `match_score` | `NUMERIC(5,2)` | NOT NULL | Composite weighted recommendation score (0.00 - 100.00) |
| `eligibility_verdict` | `VARCHAR(30)` | NOT NULL | `ELIGIBLE` or `NEEDS_VERIFICATION` |
| `eligibility_score` | `NUMERIC(5,2)` | NOT NULL | Evaluation match percentage from Eligibility Service |
| `rank_position` | `INT` | NOT NULL DEFAULT 0 | 1-indexed rank position |
| `score_breakdown` | `TEXT` | NOT NULL | JSON breakdown of factors, weights, and explanation |
| `rule_set_version` | `INT` | NOT NULL DEFAULT 1 | Rule set version used for eligibility evaluation |
| `is_dismissed` | `BOOLEAN` | NOT NULL DEFAULT FALSE | True if dismissed by the student |
| `calculated_at` | `TIMESTAMPTZ` | NOT NULL | Timestamp of recommendation calculation |

*Unique Constraint:* `uq_student_schol_rec(student_id, scholarship_id)`

### `student_preferences` Table
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PRIMARY KEY | Preference record ID |
| `student_id` | `UUID` | NOT NULL UNIQUE | Student's user ID |
| `preferred_fields` | `TEXT` | NOT NULL DEFAULT '[]' | JSON array of preferred fields of study |
| `preferred_locations` | `TEXT` | NOT NULL DEFAULT '[]' | JSON array of preferred locations |
| `min_award_preference` | `NUMERIC(12,2)` | NULL | Desired minimum award threshold |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | Last update timestamp |

---

## API Endpoints

### Student Personalized Recommendations (`/api/v1/recommendations`)
- **`GET /api/v1/recommendations`** (`ROLE_STUDENT`):
  Retrieve paginated, ranked recommendations for the authenticated student.
  - Query params: `page`, `size`, `minScore`, `includeNeedsVerification`, `recalculate`, `studentId` (Admin only).
- **`POST /api/v1/recommendations/recalculate`** (`ROLE_STUDENT`):
  Trigger on-demand recalculation against active catalog and student profile.
- **`PATCH /api/v1/recommendations/{scholarshipId}/dismiss`** (`ROLE_STUDENT`):
  Dismiss a scholarship recommendation from the active feed.
- **`PATCH /api/v1/recommendations/{scholarshipId}/undismiss`** (`ROLE_STUDENT`):
  Restore a previously dismissed recommendation.
- **`GET /api/v1/recommendations/preferences`** (`ROLE_STUDENT`):
  Retrieve student's recommendation preferences.
- **`PUT /api/v1/recommendations/preferences`** (`ROLE_STUDENT`):
  Update student's recommendation preferences and trigger recalculation.

### Protected Internal API (`/internal/v1/recommendations`)
Requires `X-Internal-Service-Key` header matching `prestiq.security.internal-service-key`:
- **`POST /internal/v1/recommendations/recalculate/{studentId}`**:
  Triggered when student profile or scholarship eligibility criteria change.
- **`GET /internal/v1/recommendations/{studentId}`**:
  Fetch recommendations for a student internally (for notification or application tracking).

---

## Verification & Testing

Run tests and package build with Java 21:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean test
./mvnw clean package
```
