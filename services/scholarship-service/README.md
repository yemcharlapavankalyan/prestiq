# Prestiq Scholarship Service (`scholarship-service`)

**Service Port:** `8083`  
**Database:** `prestiq_scholarship_db`  
**Runtime:** Java 21 LTS  
**Framework:** Spring Boot 3.3.5 / Spring Cloud 2023.0.4  

---

## 1. Responsibilities & Boundaries

The **Scholarship Service** is the authoritative catalog of verified educational opportunities, funding criteria, and official source provenance.

### Service Boundaries & Data Ownership
* **Scholarship Catalog:** Title, provider/sponsor, description, official website URL, application portal URL, award structure, min/max award amounts, and academic year.
* **Academic & Demographic Criteria:** Eligible countries (ISO 3166-1 alpha-2), states, education levels (`UNDERGRADUATE`, `POSTGRADUATE`, etc.), GPA thresholds, max income limits, and structured criteria schemas compatible with the deterministic Eligibility Service.
* **Publication & Verification Lifecycle:** Dual-status state machine enforcing that opportunities cannot be published to students unless certified and verified by an authorized officer or admin.
* **Source Provenance:** Full tracking of source URLs, content hashes (SHA-256), scrape timestamps, and HTTP response statuses.
* **Zero Cross-Database Foreign Keys:** Referential boundaries maintained via logical references (`verified_by_user_id: UUID`).

---

## 2. Scholarship Lifecycle State Machine

```
              ┌──────────────┐
              │    DRAFT     │
              └──────┬───────┘
                     │ (submit-review)
                     ▼
              ┌──────────────┐
       ┌─────►│PENDING_REVIEW│◄─────┐
       │      └──────┬───────┘      │
       │             │              │
(submit-review)      │ (verify)     │ (submit-review)
       │             ▼              │
       │      ┌──────────────┐      │
       │      │   VERIFIED   │      │
       │      └──────┬───────┘      │
       │             │ (publish)    │
       │             ▼              │
       │      ┌──────────────┐      │
       │      │  PUBLISHED   │      │
       │      └──────┬───────┘      │
       │             │ (archive)    │
       │             ▼              │
       │      ┌──────────────┐      │
       │      │   ARCHIVED   │      │
       │      └──────────────┘      │
       │                            │
       └──────┬──────────────┬──────┘
              │   REJECTED   │
              └──────────────┘
```

1. **DRAFT:** New opportunity created by admin, officer, or automated crawler worker.
2. **PENDING_REVIEW:** Submitted for human-in-the-loop (HITL) review.
3. **VERIFIED:** Officer/admin verifies the opportunity details, setting `verified_by_user_id` and `verified_at`.
4. **REJECTED:** Officer/admin rejects the opportunity with a mandatory `rejectionReason`. (Can be re-submitted for review if revised).
5. **PUBLISHED:** Only `VERIFIED` scholarships can be published. Only published and verified scholarships are accessible on public student endpoints.
6. **ARCHIVED:** Concluded, expired, or retired scholarship cycles. Archived records are removed from public discovery.

---

## 3. API Endpoints

### Public Discovery API (`/api/v1/scholarships`)
Public endpoints are unauthenticated and expose **only** verified, published opportunities.

| Method | Endpoint | Parameters / Description |
| :--- | :--- | :--- |
| `GET` | `/api/v1/scholarships` | Multi-criteria search, filters (`query`, `educationLevel`, `country`, `minAmount`, `maxAmount`, `sourceType`, `includeExpired`), pagination (`page`, `size`), and sorting (`sort=deadline,asc`, `amount,desc`, etc.). |
| `GET` | `/api/v1/scholarships/{idOrSlug}` | Detailed view of a published scholarship by numeric ID or URL slug (e.g. `/api/v1/scholarships/stem-leaders-2026`). Returns `404` for drafts or archived records. |

### Administrative API (`/api/v1/admin/scholarships`)
Requires `ROLE_ADMIN` or `ROLE_SCHOLARSHIP_OFFICER` via JWT Bearer token.

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/admin/scholarships` | Create draft scholarship. |
| `GET` | `/api/v1/admin/scholarships` | List all scholarships across any lifecycle state with admin filters. |
| `GET` | `/api/v1/admin/scholarships/{id}` | Retrieve any scholarship by ID regardless of status. |
| `PUT` | `/api/v1/admin/scholarships/{id}` | Update scholarship details, criteria, or deadlines. |
| `PUT` | `/api/v1/admin/scholarships/{id}/submit-review` | Move status to `PENDING_REVIEW`. |
| `PUT` | `/api/v1/admin/scholarships/{id}/verify` | Certify opportunity, moving status to `VERIFIED`. |
| `PUT` | `/api/v1/admin/scholarships/{id}/reject` | Reject opportunity with mandatory `rejectionReason`. |
| `PUT` | `/api/v1/admin/scholarships/{id}/publish` | Publish verified scholarship. Rejects with `409` if not verified. |
| `PUT` | `/api/v1/admin/scholarships/{id}/archive` | Archive scholarship with optional `archivedReason`. |
| `POST` | `/api/v1/admin/scholarships/{id}/sources` | Register a provenance source URL, scrape timestamp, and SHA-256 hash. |
| `GET` | `/api/v1/admin/scholarships/{id}/sources` | List all registered provenance sources. |
| `DELETE` | `/api/v1/admin/scholarships/{id}/sources/{sourceId}` | Remove a provenance source. |

---

## 4. Sorting Allowlist & Security

To prevent SQL injection and property enumeration, the `sort` query parameter strictly permits only fields in the allowlist:
* `deadline` / `applicationDeadline` (`applicationDeadline`)
* `amount` / `awardAmount` (`awardAmountMax`)
* `title` (`title`)
* `createdAt` (`createdAt`)
* `provider` / `providerName` (`providerName`)

Any attempt to sort by unlisted properties returns `400 Bad Request` with an RFC 9457 `ProblemDetail`.

---

## 5. Configuration & Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SCHOLARSHIP_SERVICE_PORT` | HTTP port | `8083` |
| `SCHOLARSHIP_DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/prestiq_scholarship_db` |
| `SCHOLARSHIP_DB_USERNAME` | Database username | `prestiq_user` |
| `SCHOLARSHIP_DB_PASSWORD` | Database password | `prestiq_password` |
| `JWT_SECRET` | HMAC-SHA256 signing secret (min 256 bits) | Shared secret |
| `JWT_ISSUER` | Expected JWT issuer | `prestiq-auth-service` |
| `INTERNAL_SERVICE_KEY`| Inter-service pre-shared key | `prestiq-internal-dev-secret-key-2026` |

---

## 6. Development & Testing Workflow

### Hardware Optimization (MacBook Air M2, 8 GB RAM)
Configured in [`.mvn/jvm.config`](.mvn/jvm.config):
```properties
-Xms64m -Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1
```

### Running Tests
Execute the isolated test suite using H2 in PostgreSQL mode (Flyway schema validation included):
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw clean test
```

### Running the Service Locally
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export SCHOLARSHIP_DB_URL=jdbc:postgresql://localhost:5432/prestiq_scholarship_db
export SCHOLARSHIP_DB_USERNAME=prestiq_user
export SCHOLARSHIP_DB_PASSWORD=prestiq_password
./mvnw spring-boot:run
```

### Swagger UI & OpenAPI Documentation
* **Swagger UI:** `http://localhost:8083/swagger-ui.html`
* **OpenAPI 3.0 JSON:** `http://localhost:8083/v3/api-docs`
