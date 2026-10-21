# Prestiq Student Service (`student-service`)

**Service Port:** `8082`  
**Database:** `prestiq_student_db`  
**Runtime:** Java 21 LTS  
**Framework:** Spring Boot 3.3.5 / Spring Cloud 2023.0.4  

---

## 1. Responsibilities & Boundaries

The **Student Service** owns all student profile data, academic credentials, and verified eligibility-related attributes.

### Service Boundaries & Data Ownership
* **Student Profiles:** Full name, date of birth, domicile, country of citizenship (ISO 3166-1 alpha-2).
* **Academic Qualifications:** Current education level, enrolled institution, major/field of study, current year/semester, cumulative GPA, and grading scale.
* **Financial & Eligibility Attributes:** Voluntarily provided household annual income, first-generation college student status, military veteran status, disability status, and extensible eligibility attributes (`additional_attributes`).
* **Identity Ownership Boundary:** The **Auth Service** remains the authoritative owner of identities, email uniqueness, passwords, roles, and tokens. `student-service` maintains referential integrity using immutable external UUID references (`user_id`). **No passwords or duplicate authentication records are stored.**

---

## 2. API Endpoints

All endpoints support both `/api/students` and `/api/v1/students` path prefixes for compatibility with the architecture specification.

| Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/students/me` | `ROLE_STUDENT`, `ROLE_ADMIN` | Retrieves current authenticated student's profile. Returns `404` if not created yet. |
| `PUT` | `/api/students/me` | `ROLE_STUDENT`, `ROLE_ADMIN` | Upserts current student's profile (creates if not existing, updates if existing). |
| `POST` | `/api/students/me` | `ROLE_STUDENT`, `ROLE_ADMIN` | Explicitly creates a profile. Returns `409 Conflict` if a profile already exists for this user. |
| `GET` | `/api/students/{studentId}` | `ROLE_ADMIN` or Profile Owner | Retrieves student profile by profile UUID. Other students attempting cross-user access receive `403 Forbidden`. |
| `GET` | `/internal/v1/students/{userId}/profile` | Internal Service Key / Admin | High-performance inter-service endpoint for `eligibility-service` and `recommendation-service`. |

---

## 3. Security Architecture: Standalone Dev vs Gateway Integration

### Current Phase (Standalone Local-Development Mode)
Because the API Gateway is not yet implemented, `student-service` enforces security directly:
1. Direct requests must supply an `Authorization: Bearer <JWT>` header signed with the shared secret (`prestiq.security.jwt.secret`).
2. The `JwtAuthenticationFilter` validates token signature, expiration, and issuer using JJWT.
3. Authenticated user ID is extracted from the cryptographically verified JWT claims (`sub`), **completely ignoring any client-supplied `X-User-Id` header to prevent header spoofing/impersonation**.
4. Cross-user access is strictly blocked: a student user cannot read another student's profile via `GET /api/students/{studentId}`.

### Future Phase (API Gateway Integration)
When the Spring Cloud Gateway (`api-gateway`) is activated:
1. **Edge Authentication:** The Gateway will validate client JWTs, authenticate incoming sessions, and strip any client-supplied `X-User-*` headers.
2. **Internal Identity Propagation:** The Gateway forwards trusted claims as internal headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`) accompanied by a pre-shared cryptographic key (`X-Internal-Service-Key`).
3. **Impersonation Prevention:** Direct requests attempting to pass `X-User-Id` without a valid `X-Internal-Service-Key` are automatically rejected.

---

## 4. Configuration & Environment Variables

| Variable | Description | Default |
| :--- | :--- | :--- |
| `STUDENT_SERVICE_PORT` | HTTP port | `8082` |
| `STUDENT_DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/prestiq_student_db` |
| `STUDENT_DB_USERNAME` | Database username | `prestiq_user` |
| `STUDENT_DB_PASSWORD` | Database password | `prestiq_password` |
| `JWT_SECRET` | HMAC-SHA256 signing secret (min 256 bits) | Shared secret |
| `JWT_ISSUER` | Expected JWT issuer | `prestiq-auth-service` |
| `INTERNAL_SERVICE_KEY`| Inter-service pre-shared key | `prestiq-internal-dev-secret-key-2026` |

---

## 5. Development & Testing Workflow

### Hardware Optimization (MacBook Air M2, 8 GB RAM)
All Maven invocations utilize tiered compilation and bounded heap configurations via `.mvn/jvm.config`:
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
export STUDENT_DB_URL=jdbc:postgresql://localhost:5432/prestiq_student_db
export STUDENT_DB_USERNAME=prestiq_user
export STUDENT_DB_PASSWORD=prestiq_password
./mvnw spring-boot:run
```

### Swagger UI & OpenAPI Documentation
* **Swagger UI:** `http://localhost:8082/swagger-ui.html`
* **OpenAPI 3.0 JSON:** `http://localhost:8082/v3/api-docs`
