# Prestiq Database Architecture & Service Ownership Specification

**Document Version:** 2.0.0  
**Architecture Pattern:** Database-per-Service (Logical Isolation)  
**Target RDBMS:** PostgreSQL 16+ (with `pgvector` extension)  
**Migration Tools:** Flyway (Java Spring Boot Services) & Alembic (Python Services)  

---

## 1. Database-per-Service Architecture & Boundary Rules

Prestiq enforces the **Database-per-Service** pattern to maintain autonomous deployment and prevent tight coupling.

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   auth-service  │       │ student-service │       │ scholarship-svc │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │ (JDBC)                  │ (JDBC)                  │ (JDBC)
         ▼                         ▼                         ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ prestiq_auth_db │       │prestiq_studentdb│       │prestiq_schol_db │
└─────────────────┘       └─────────────────┘       └─────────────────┘
```

### 1.1 Structural Ownership Rules
1. **Zero Cross-Service Table Writes or Reads:** No service may query or alter another service's tables. All cross-domain data exchanges occur via REST APIs or events.
2. **No Cross-Database Foreign Keys:** Referential integrity across services is maintained at the application/API layer using immutable IDs (`studentId: UUID`, `scholarshipId: Long`).
3. **Dedicated Connection Credentials:** In production, each service connects with a dedicated PostgreSQL user permitted only to access its specific database/schema.
4. **Independent Migration Histories:** Each service maintains its own `flyway_schema_history` or Alembic version table.

---

## 2. Microservice Database Specifications

### 2.1 `auth-service` Database (`prestiq_auth_db`)

Manages user identity, credentials, roles, and token revocation.

#### `users`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Immutable user ID |
| `email` | `VARCHAR(255)` | `NOT NULL UNIQUE` | Normalized lowercase email |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | Argon2id cryptographic hash |
| `is_active` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | Account status flag |
| `is_verified` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Email confirmation flag |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Last update timestamp |

#### `roles` & `user_roles`
* `roles`: `id INT PRIMARY KEY`, `name VARCHAR(50) UNIQUE` (`ROLE_STUDENT`, `ROLE_SCHOLARSHIP_OFFICER`, `ROLE_ADMIN`).
* `user_roles`: `user_id UUID REFERENCES users(id) ON DELETE CASCADE`, `role_id INT REFERENCES roles(id)`, `PRIMARY KEY (user_id, role_id)`.

#### `refresh_tokens`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Refresh token record ID |
| `user_id` | `UUID` | `NOT NULL REFERENCES users(id) ON DELETE CASCADE` | Associated user |
| `token_hash` | `VARCHAR(64)` | `NOT NULL UNIQUE` | SHA-256 hash of refresh secret |
| `expires_at` | `TIMESTAMPTZ` | `NOT NULL` | Absolute expiration date (7 days) |
| `is_revoked` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Explicit revocation marker |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Issuance timestamp |

---

### 2.2 `student-service` Database (`prestiq_student_db`)

Houses student academic qualifications, demographics, and self-reported criteria.

#### `student_profiles`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY DEFAULT gen_random_uuid()` | Student Profile ID |
| `user_id` | `UUID` | `NOT NULL UNIQUE` | User ID from `auth-service` (Logical FK) |
| `first_name` | `VARCHAR(100)` | `NOT NULL` | First name |
| `last_name` | `VARCHAR(100)` | `NOT NULL` | Last name |
| `date_of_birth` | `DATE` | `NULL` | Student birth date (age checks) |
| `citizenship_country` | `VARCHAR(2)` | `NOT NULL` | ISO 3166-1 alpha-2 code (e.g. `US`, `IN`) |
| `state_province` | `VARCHAR(100)` | `NULL` | Domicile state/province |
| `education_level` | `VARCHAR(50)` | `NOT NULL` | `HIGH_SCHOOL`, `UNDERGRADUATE`, `POSTGRADUATE`, `DOCTORAL` |
| `institution_name` | `VARCHAR(255)` | `NULL` | Current enrolled university/school |
| `major_field_of_study`| `VARCHAR(150)` | `NULL` | Academic discipline/major |
| `cumulative_gpa` | `NUMERIC(4,2)`| `NULL` | Current GPA (normalized or scaled) |
| `gpa_scale` | `NUMERIC(4,2)`| `NOT NULL DEFAULT 4.00` | GPA scale (4.00, 5.00, 10.00) |
| `annual_family_income`| `NUMERIC(14,2)`| `NULL` | Household income for need-based checks |
| `currency_code` | `VARCHAR(3)` | `NOT NULL DEFAULT 'USD'` | ISO 4217 currency code |
| `first_gen_student` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | First-generation college student |
| `military_veteran` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Veteran/military status |
| `disability_status` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Self-reported disability status |
| `additional_attributes`| `JSONB` | `NOT NULL DEFAULT '{}'::jsonb` | Extensible key-value eligibility attributes |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Update timestamp |

---

### 2.3 `scholarship-service` Database (`prestiq_scholarship_db`)

Authoritative catalog of verified opportunities, sources, and audit logs.

#### `scholarships`
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Catalog identifier |
| `slug` | `VARCHAR(200)` | `NOT NULL UNIQUE` | URL slug |
| `title` | `VARCHAR(255)` | `NOT NULL` | Official scholarship name |
| `provider_name` | `VARCHAR(255)` | `NOT NULL` | Organization/institution offering award |
| `description` | `TEXT` | `NOT NULL` | Verified summary & description |
| `official_website_url`| `VARCHAR(1000)`| `NOT NULL` | Canonical application website |
| `source_type` | `VARCHAR(50)` | `NOT NULL` | `GOVERNMENT`, `UNIVERSITY`, `PRIVATE_FOUNDATION`, `CORPORATE` |
| `verification_status` | `VARCHAR(50)` | `NOT NULL DEFAULT 'DRAFT'` | `DRAFT`, `PENDING_REVIEW`, `VERIFIED`, `REJECTED`, `ARCHIVED` |
| `verified_by_user_id`| `UUID` | `NULL` | Admin User ID from `auth-service` |
| `verified_at` | `TIMESTAMPTZ` | `NULL` | Verification timestamp |
| `award_amount_min` | `NUMERIC(12,2)`| `NULL` | Minimum grant amount |
| `award_amount_max` | `NUMERIC(12,2)`| `NULL` | Maximum grant amount |
| `currency_code` | `VARCHAR(3)` | `NOT NULL DEFAULT 'USD'` | Currency code |
| `is_renewable` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` | Renewable flag |
| `application_deadline`| `DATE` | `NOT NULL` | Official deadline |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT clock_timestamp()` | Update timestamp |

#### `scholarship_sources`
Tracks original source URLs, scrape timestamps, and content hashes for change detection.
* `id BIGSERIAL PRIMARY KEY`
* `scholarship_id BIGINT NOT NULL REFERENCES scholarships(id) ON DELETE CASCADE`
* `source_url VARCHAR(1000) NOT NULL`
* `source_name VARCHAR(255) NOT NULL`
* `content_hash VARCHAR(64) NOT NULL` (SHA-256)
* `last_scraped_at TIMESTAMPTZ NOT NULL`
* `http_status INT NOT NULL`

---

### 2.4 `eligibility-service` Database (`prestiq_eligibility_db`)

Houses versioned rule sets and cached evaluation results.

#### `scholarship_rule_sets`
* `id BIGSERIAL PRIMARY KEY`
* `scholarship_id BIGINT NOT NULL` (Logical reference to `scholarship-service`)
* `version INT NOT NULL DEFAULT 1`
* `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'` (`DRAFT`, `ACTIVE`, `SUPERSEDED`)
* `created_by_user_id UUID NOT NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `CONSTRAINT uq_rule_version UNIQUE(scholarship_id, version)`

#### `eligibility_criteria`
* `id BIGSERIAL PRIMARY KEY`
* `rule_set_id BIGINT NOT NULL REFERENCES scholarship_rule_sets(id) ON DELETE CASCADE`
* `criterion_code VARCHAR(50) NOT NULL` (e.g. `MIN_GPA`, `CITIZENSHIP`, `INCOME_MAX`)
* `target_field VARCHAR(100) NOT NULL` (e.g. `academic.cumulativeGpa`)
* `operator VARCHAR(30) NOT NULL` (`GTE`, `LTE`, `EQ`, `IN`, `CONTAINS_ANY`, `BETWEEN`)
* `target_value JSONB NOT NULL`
* `is_mandatory BOOLEAN NOT NULL DEFAULT TRUE`
* `display_name VARCHAR(255) NOT NULL`
* `failure_message VARCHAR(500) NOT NULL`

#### `eligibility_evaluations`
* `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
* `student_id UUID NOT NULL` (Logical reference to `student-service`)
* `scholarship_id BIGINT NOT NULL` (Logical reference to `scholarship-service`)
* `rule_set_version INT NOT NULL`
* `verdict VARCHAR(30) NOT NULL` (`ELIGIBLE`, `INELIGIBLE`, `NEEDS_VERIFICATION`)
* `match_percentage NUMERIC(5,2) NOT NULL`
* `criteria_breakdown JSONB NOT NULL`
* `evaluated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`

---

### 2.5 `recommendation-service` Database (`prestiq_recommendation_db`)

Stores personalized recommendation match scores, student preferences, and cache records.

#### `student_recommendations`
* `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
* `student_id UUID NOT NULL` (Logical reference to `student-service`)
* `scholarship_id BIGINT NOT NULL` (Logical reference to `scholarship-service`)
* `match_score NUMERIC(5,2) NOT NULL` (Calculated recommendation rank score)
* `is_dismissed BOOLEAN NOT NULL DEFAULT FALSE`
* `calculated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `CONSTRAINT uq_student_schol_rec UNIQUE(student_id, scholarship_id)`

#### `student_preferences`
* `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
* `student_id UUID NOT NULL UNIQUE` (Logical reference to `student-service`)
* `preferred_fields JSONB NOT NULL DEFAULT '[]'::jsonb`
* `preferred_locations JSONB NOT NULL DEFAULT '[]'::jsonb`
* `min_award_preference NUMERIC(12,2) NULL`
* `updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`

---

### 2.6 `application-service` Database (`prestiq_application_db`)

Tracks student applications, stages, and status transitions.

#### `scholarship_applications`
* `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
* `student_id UUID NOT NULL`
* `scholarship_id BIGINT NOT NULL`
* `current_stage VARCHAR(50) NOT NULL DEFAULT 'DRAFT'` (`DRAFT`, `IN_PROGRESS`, `SUBMITTED_EXTERNALLY`, `AWARDED`, `REJECTED`, `WITHDRAWN`)
* `external_portal_reference VARCHAR(255) NULL`
* `submission_deadline DATE NOT NULL`
* `actual_submission_date DATE NULL`
* `award_amount_received NUMERIC(12,2) NULL`
* `notes TEXT NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `CONSTRAINT uq_student_scholarship_app UNIQUE(student_id, scholarship_id)`

#### `application_status_history`
* `id BIGSERIAL PRIMARY KEY`
* `application_id UUID NOT NULL REFERENCES scholarship_applications(id) ON DELETE CASCADE`
* `from_stage VARCHAR(50) NULL`
* `to_stage VARCHAR(50) NOT NULL`
* `changed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `notes TEXT NULL`

#### `saved_scholarships`
* `id BIGSERIAL PRIMARY KEY`
* `student_id UUID NOT NULL`
* `scholarship_id BIGINT NOT NULL`
* `notes TEXT NULL`
* `created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`
* `CONSTRAINT uq_student_saved_scholarship UNIQUE(student_id, scholarship_id)`

---

### 2.7 `notification-service` Database (`prestiq_notification_db`)

Tracks deadline reminders and delivery dispatches.

#### `deadline_reminders`
* `id BIGSERIAL PRIMARY KEY`
* `application_id UUID NOT NULL` (Logical reference to `application-service`)
* `student_id UUID NOT NULL`
* `remind_at TIMESTAMPTZ NOT NULL`
* `reminder_type VARCHAR(50) NOT NULL` (`30_DAYS_BEFORE`, `7_DAYS_BEFORE`, `1_DAY_BEFORE`)
* `status VARCHAR(30) NOT NULL DEFAULT 'PENDING'` (`PENDING`, `DISPATCHED`, `CANCELLED`, `FAILED`)
* `dispatched_at TIMESTAMPTZ NULL`

---

### 2.8 `ai-service` Database (`prestiq_ai_db`)

Operates vector embeddings and document chunks using `pgvector`.

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

#### `document_chunks`
* `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
* `scholarship_id BIGINT NOT NULL` (Logical reference to `scholarship-service`)
* `source_id BIGINT NOT NULL`
* `chunk_index INT NOT NULL`
* `content TEXT NOT NULL`
* `token_count INT NOT NULL`
* `heading_path VARCHAR(500) NULL`
* `source_url VARCHAR(1000) NOT NULL`
* `embedding vector(768)` (Default for Google Gemini `text-embedding-004` / `gemini-embedding-001`, or `vector(1536)` if configured)
* `created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`

**HNSW Index for High-Speed Approximate Nearest Neighbor Search:**
```sql
CREATE INDEX idx_chunks_embedding_hnsw ON document_chunks 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_chunks_scholarship_id ON document_chunks(scholarship_id);
```

---

### 2.9 `scholarship-ingestion-service` Database (`prestiq_ingestion_db`)

Tracks raw scraped HTML/PDF snapshots, job queues, and parsing status.

#### `scraped_snapshots`
* `id BIGSERIAL PRIMARY KEY`
* `source_url VARCHAR(1000) NOT NULL`
* `content_hash VARCHAR(64) NOT NULL` (SHA-256)
* `raw_content TEXT NOT NULL`
* `content_type VARCHAR(100) NOT NULL`
* `http_status INT NOT NULL`
* `scraped_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`

---

## 3. Migration Strategy (Flyway & Alembic)

1. **Java Spring Boot Services (`auth`, `student`, `scholarship`, `eligibility`, `recommendation`, `application`, `notification`):**
   * Use **Flyway** with migrations placed in `src/main/resources/db/migration/V1__init.sql`.
   * Migrations run automatically on service startup against the service's own database.
2. **Python Services (`ai-service`, `ingestion-service`):**
   * Use **Alembic** (or async raw SQL migration scripts executed via `asyncpg`) to maintain `document_chunks` and `scraped_snapshots`.
3. **Local Setup Automation:**
   * A root shell script `scripts/setup-local-databases.sh` connects once to native PostgreSQL on macOS to create the 9 logical databases and install `vector` extension.
