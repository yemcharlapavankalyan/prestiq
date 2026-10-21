# Prestiq Application Service

Scholarship application lifecycle tracking, stage management, status audit history, deadline monitoring, and saved scholarship bookmarking service for the Prestiq platform.

## Overview

The **Application Service** is the authoritative domain owner of:
1. Student scholarship application lifecycle tracking (`scholarship_applications`)
2. Structured stage transitions (`PLANNING`, `IN_PROGRESS`, `READY_TO_SUBMIT`, `SUBMITTED`, `UNDER_REVIEW`, `AWARDED`, `REJECTED`, `WITHDRAWN`)
3. Complete chronological status transition audit trails (`application_status_history`)
4. Upcoming submission deadline monitoring and dispatch triggers
5. Student saved/bookmarked scholarships (`saved_scholarships`)

### Key Principles & Constraints
- **Explicit Submission:** Applications are never marked `SUBMITTED` merely by viewing or opening an external scholarship portal. Status progression to `SUBMITTED` must be an explicit student confirmation or verified API integration.
- **Sensible Duplicate Application Rules:** A student may have at most **one active application** per scholarship (`PLANNING`, `IN_PROGRESS`, `READY_TO_SUBMIT`, `SUBMITTED`, `UNDER_REVIEW`). Attempting to create a duplicate active application yields HTTP `409 Conflict`. However, if an earlier application was `WITHDRAWN` or `REJECTED`, the student is permitted to apply again for subsequent cycles.
- **No Cross-Database Foreign Keys:** Logical references only: `student_id` (UUID reference to `student-service`) and `scholarship_id` (BIGINT reference to `scholarship-service`).
- **Student Ownership & FERPA Isolation:** Strict service-layer isolation ensures students cannot read, modify, or withdraw applications belonging to other students.

---

## Technical Stack

- **Java Version:** Java 21 LTS
- **Framework:** Spring Boot 3.3.5
- **Build Tool:** Maven with Maven Wrapper (`./mvnw`)
- **Port:** `8086`
- **Database:** PostgreSQL (`prestiq_application_db`)
- **Schema Management:** Flyway
- **Security:** Spring Security + JJWT (0.12.6) + Internal Service Key Validation
- **Memory Footprint:** Bounded JVM heap (`-Xms64m -Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1`) optimized for an 8 GB MacBook Air M2
- **Documentation:** OpenAPI 3.0 (Springdoc 2.6.0)

---

## Application Lifecycle & Allowed Status Transitions

```
                    ┌─────────────────────────┐
                    │        PLANNING         │◄────────┐
                    └───────────┬─────────────┘         │
                                │                       │
                                ▼                       │
                    ┌─────────────────────────┐         │
                    │       IN_PROGRESS       │         │
                    └───────────┬─────────────┘         │
                                │                       │
                                ▼                       │
                    ┌─────────────────────────┐         │ (Re-open)
                    │     READY_TO_SUBMIT     │         │
                    └───────────┬─────────────┘         │
                                │                       │
                                ▼                       │
                    ┌─────────────────────────┐         │
                    │        SUBMITTED        │         │
                    └─────┬─────────────┬─────┘         │
                          │             │               │
                          ▼             │               │
                    ┌───────────┐       │               │
                    │UNDER_REVIEW│      │               │
                    └─────┬─────┘       │               │
                          │             │               │
             ┌────────────┴─────────────┴────────┐      │
             ▼                                   ▼      │
      ┌───────────────┐                  ┌───────────────┴─┐
      │    AWARDED    │                  │  REJECTED /     │
      │   (Terminal)  │                  │  WITHDRAWN      │
      └───────────────┘                  └─────────────────┘
```

### Allowed Transitions Matrix
| Current Status | Allowed Next Statuses | Notes |
|---|---|---|
| `PLANNING` | `IN_PROGRESS`, `WITHDRAWN` | Initial state on creation |
| `IN_PROGRESS` | `PLANNING`, `READY_TO_SUBMIT`, `WITHDRAWN` | Preparation of materials, essays, letters |
| `READY_TO_SUBMIT`| `IN_PROGRESS`, `SUBMITTED`, `WITHDRAWN` | Checklist complete; student explicitly confirms submission |
| `SUBMITTED` | `UNDER_REVIEW`, `AWARDED`, `REJECTED`, `WITHDRAWN` | Captures `submitted_at`, `actual_submission_date`, 100% progress |
| `UNDER_REVIEW` | `AWARDED`, `REJECTED`, `WITHDRAWN` | Provider review period |
| `AWARDED` | *(None - Terminal)* | Captures `award_amount_received` |
| `REJECTED` | *(None - Terminal)* | Application not selected |
| `WITHDRAWN` | `PLANNING` | Can be re-opened for planning a future re-application |

*Invalid transition attempts immediately return `409 Conflict` (ProblemDetail type `invalid-status-transition`).*

---

## Database Schema (`prestiq_application_db`)

### 1. `scholarship_applications`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `UUID` | PRIMARY KEY | Application ID |
| `student_id` | `UUID` | NOT NULL | Student's user ID |
| `scholarship_id` | `BIGINT` | NOT NULL | Scholarship catalog ID |
| `scholarship_title` | `VARCHAR(255)` | NOT NULL | Snapshot of scholarship title at application time |
| `scholarship_provider` | `VARCHAR(255)` | NULL | Snapshot of provider organization |
| `current_status` | `VARCHAR(50)` | NOT NULL DEFAULT 'PLANNING' | Current lifecycle status |
| `external_portal_reference` | `VARCHAR(255)` | NULL | Reference number or external portal application URL |
| `submission_deadline` | `DATE` | NOT NULL | Application deadline snapshot |
| `actual_submission_date` | `DATE` | NULL | Date student confirmed submission |
| `submitted_at` | `TIMESTAMPTZ` | NULL | Exact timestamp of submission status change |
| `award_amount_received` | `NUMERIC(12,2)` | NULL | Actual awarded amount if status is AWARDED |
| `progress_percentage` | `INT` | NOT NULL DEFAULT 0 | 0 to 100 checklist completion percentage |
| `notes` | `TEXT` | NULL | Student notes and essay brainstorms |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | Timestamp created |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL | Timestamp last updated |

### 2. `application_status_history`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY | History entry ID |
| `application_id` | `UUID` | NOT NULL REFERENCES `scholarship_applications(id)` ON DELETE CASCADE | Associated application |
| `from_status` | `VARCHAR(50)` | NULL | Previous status (null on initialization) |
| `to_status` | `VARCHAR(50)` | NOT NULL | New status |
| `changed_by_user_id` | `UUID` | NOT NULL | User who initiated transition |
| `changed_at` | `TIMESTAMPTZ` | NOT NULL | Timestamp of status change |
| `notes` | `TEXT` | NULL | Audit or transition reason notes |

### 3. `saved_scholarships`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | `BIGSERIAL` | PRIMARY KEY | Bookmark ID |
| `student_id` | `UUID` | NOT NULL | Student's user ID |
| `scholarship_id` | `BIGINT` | NOT NULL | Bookmarked scholarship ID |
| `scholarship_title` | `VARCHAR(255)` | NULL | Snapshot of title |
| `scholarship_provider` | `VARCHAR(255)` | NULL | Snapshot of provider |
| `application_deadline` | `DATE` | NULL | Deadline snapshot |
| `award_amount_max` | `NUMERIC(12,2)` | NULL | Maximum award snapshot |
| `notes` | `TEXT` | NULL | Bookmark notes |
| `created_at` | `TIMESTAMPTZ` | NOT NULL | Timestamp bookmarked |

*Unique Constraint:* `uq_student_saved_scholarship(student_id, scholarship_id)`

---

## API Endpoints

### Scholarship Applications (`/api/v1/applications`)
- **`POST /api/v1/applications`** (`ROLE_STUDENT`): Start tracking an application.
- **`GET /api/v1/applications`** (`ROLE_STUDENT`): List applications for the authenticated student (pagination, status filtering, sorting).
- **`GET /api/v1/applications/{id}`** (`ROLE_STUDENT`): Retrieve application by ID with ownership verification.
- **`PATCH /api/v1/applications/{id}`** (`ROLE_STUDENT`): Update checklist progress (0-100), notes, or external portal reference.
- **`PATCH /api/v1/applications/{id}/status`** (`ROLE_STUDENT`): Transition status through validated lifecycle.
- **`POST /api/v1/applications/{id}/withdraw`** (`ROLE_STUDENT`): Voluntarily withdraw an application.
- **`GET /api/v1/applications/{id}/history`** (`ROLE_STUDENT`): Retrieve chronological audit history.
- **`GET /api/v1/applications/deadlines`** (`ROLE_STUDENT`): View upcoming active deadlines (within `daysAhead` days, default: 30).

### Saved Scholarships Bookmarks (`/api/v1/saved-scholarships`)
- **`POST /api/v1/saved-scholarships/{scholarshipId}`** (`ROLE_STUDENT`): Bookmark a scholarship.
- **`GET /api/v1/saved-scholarships`** (`ROLE_STUDENT`): List bookmarked scholarships.
- **`DELETE /api/v1/saved-scholarships/{scholarshipId}`** (`ROLE_STUDENT`): Remove a bookmark.

### Protected Internal API (`/internal/v1/applications`)
Requires `X-Internal-Service-Key` header matching `prestiq.security.internal-service-key`:
- **`GET /internal/v1/applications/{id}`**: Internal retrieval of an application.
- **`GET /internal/v1/applications/student/{studentId}`**: Internal retrieval of student applications.

---

## Testing & Database Compatibility

Run tests with Java 21:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean test
./mvnw clean package
```

### PostgreSQL vs H2 Note
In PostgreSQL production/staging, the partial index `CREATE UNIQUE INDEX uq_student_active_application ON scholarship_applications(student_id, scholarship_id) WHERE current_status NOT IN ('WITHDRAWN', 'REJECTED');` can enforce active deduplication at the database level. For cross-database compatibility with H2 in test mode, the primary deduplication check is enforced transactionally at the application service layer via `existsByStudentIdAndScholarshipIdAndCurrentStatusNotIn()`.
