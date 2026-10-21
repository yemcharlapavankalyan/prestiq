# Prestiq Auth Service (`auth-service`)

The **Auth Service** is the central Identity and Access Management (IAM) authority for the Prestiq platform. It manages user accounts, password credentials (Argon2id), JWT access tokens, rotating refresh tokens, and security audit logs.

---

## 1. Specifications & Technology Stack

* **Port:** `8081` (configurable via `AUTH_SERVICE_PORT`)
* **Java Version:** Java 21 LTS
* **Spring Boot Version:** `3.3.5`
* **Database:** `prestiq_auth_db` (PostgreSQL 16)
* **Migrations:** Flyway (`src/main/resources/db/migration/`)
* **Password Hashing:** Argon2id (OWASP recommended parameters via Bouncy Castle)
* **Token Standard:** Stateless JWT (HMAC-SHA256, 15-minute expiration) + HttpOnly rotating refresh tokens (7 days)
* **Error Standard:** RFC 9457 Problem Details for HTTP APIs

---

## 2. API Endpoints

All endpoints are mapped to both `/api/auth` and `/api/v1/auth`:

| Method | Path | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | `PUBLIC` | Register new student account; sets HttpOnly refresh cookie. |
| `POST` | `/api/auth/login` | `PUBLIC` | Authenticate email/password; returns JWT and sets refresh cookie. |
| `POST` | `/api/auth/refresh` | `PUBLIC` | Single-use refresh token exchange with compromise reuse detection. |
| `POST` | `/api/auth/logout` | `PUBLIC` | Revokes server-side refresh token and clears cookie. |
| `GET` | `/api/auth/me` | `ROLE_STUDENT`, `ROLE_ADMIN` | Returns profile for authenticated token bearer. |

---

## 3. Local Development Execution (8 GB MacBook Air M2)

### Prerequisites
* Native PostgreSQL running on `localhost:5432` (`brew services start postgresql@16`).
* Database `prestiq_auth_db` initialized (via `scripts/setup-local-databases.sh`).
* Java 21 active.

### Running with Maven Wrapper
From `services/auth-service`:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw spring-boot:run
```

### Stopping the Service
Press `Ctrl+C` in the terminal, or run:
```bash
kill $(lsof -t -i:8081)
```

---

## 4. Verification & Testing

### Running the Test Suite
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw test
```
The test suite utilizes an isolated in-memory H2 database (PostgreSQL mode) to run unit and integration tests without touching production or local development databases.

### Swagger UI & OpenAPI 3.0 Documentation
Once started, visit:
* **Interactive UI:** `http://localhost:8081/swagger-ui.html`
* **OpenAPI JSON Spec:** `http://localhost:8081/v3/api-docs`

---

## 5. Security & Deployment Notes

1. **Password Hashing:** Uses Argon2id (`Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`). Memory consumption is tuned for secure verification without exhausting local RAM.
2. **Refresh Token Compromise Detection:** If an already-rotated or revoked token is presented, the system assumes token theft and immediately terminates all active sessions for that user.
3. **Cookie Security:** In local development, `COOKIE_SECURE=false` allows testing over HTTP. In production on Railway/Render, set `COOKIE_SECURE=true` and `COOKIE_SAMESITE=Strict`.
4. **Zero Hardcoded Secrets:** Set `JWT_SECRET` (256-bit+ secure random secret) in `.env` before production deployment.
