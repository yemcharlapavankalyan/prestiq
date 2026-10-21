# PrestIQ API Gateway (`api-gateway`)

**Port:** `8080`  
**Runtime:** Java 21 / Spring Boot 3.3.5 / Spring Cloud Gateway 2023.0.4 (Reactive WebFlux / Netty)  
**Discovery:** Spring Cloud Netflix Eureka Client (`http://localhost:8761/eureka/`)

---

## 1. Overview & Architectural Role

The **PrestIQ API Gateway** acts as the unified reverse proxy, edge security boundary, and dynamic routing hub for all public API traffic across the PrestIQ microservices platform.

### Core Capabilities
1. **Explicit Route Registry:** Zero wildcard or indiscriminate routing; only explicit, documented contracts are routed to downstream services.
2. **Service Discovery Integration:** Resolves downstream endpoints dynamically via Eureka (`lb://<service-name>`) for Spring Boot services, with direct URL fallback (`AI_SERVICE_URL`, `INGESTION_SERVICE_URL`).
3. **Edge Security & Header Sanitization:**
   - Strips untrusted identity headers (`X-User-*`, `X-Student-*`) and forged internal keys (`X-Internal-Service-Key`) on all incoming requests.
   - Enforces unique distributed tracing (`X-Request-Id`).
   - Forwards verified identity claims (`X-User-Id`, `X-User-Email`, `X-User-Roles`) along with internal credentials (`X-Internal-Service-Key`) to downstream services while preserving the original `Authorization: Bearer <token>`.
4. **Internal Route Isolation:** Completely blocks external access to any internal service paths (`/internal/**`) with RFC 7807 problem details (`403 Forbidden`).
5. **Strict CORS Policy:** Explicitly allows configured development and production origins (e.g., `http://localhost:3000`, `http://localhost:5173`) with credentials enabled, strictly rejecting arbitrary or wildcard origins.
6. **Bounded Resilience & Timeouts:** Applies bounded connection timeouts (3s default, 5s AI) and response timeouts (5s default, 15s AI synthesis) to prevent cascade failures.
7. **RFC 7807 Error Normalization:** Translates downstream connection drops (503 Service Unavailable), timeouts (504 Gateway Timeout), and route misses (404 Not Found) into clean, standard problem details without leaking internal topology.

---

## 2. Gateway Route Table

| Route ID | Incoming Path Predicate | Destination URI | Protocol / Discovery | Timeout | Security Boundary |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `auth-service` | `/api/v1/auth/**`, `/api/auth/**` | `lb://auth-service` (`:8081`) | Eureka Load Balanced | 5s / 3s | Public (`/register`, `/login`, `/refresh`), Authenticated (`/logout`) |
| `student-service` | `/api/v1/students/**`, `/api/students/**` | `lb://student-service` (`:8082`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` (`/me/profile`), `ROLE_ADMIN` (`/{id}/profile`) |
| `scholarship-service-public` | `/api/v1/scholarships/**` | `lb://scholarship-service` (`:8083`) | Eureka Load Balanced | 5s / 3s | Public catalog search, filter, and detail viewing |
| `scholarship-service-admin` | `/api/v1/admin/scholarships/**` | `lb://scholarship-service` (`:8083`) | Eureka Load Balanced | 5s / 3s | `ROLE_ADMIN` verification and catalog moderation |
| `eligibility-service` | `/api/v1/eligibility/**` | `lb://eligibility-service` (`:8084`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` deterministic evaluation |
| `eligibility-service-admin` | `/api/v1/admin/eligibility/**` | `lb://eligibility-service` (`:8084`) | Eureka Load Balanced | 5s / 3s | `ROLE_ADMIN` rule set management |
| `recommendation-service` | `/api/v1/recommendations/**` | `lb://recommendation-service` (`:8085`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` personalized ranking |
| `application-service` | `/api/v1/applications/**` | `lb://application-service` (`:8086`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` application lifecycle tracking |
| `saved-scholarships-service` | `/api/v1/saved-scholarships/**` | `lb://application-service` (`:8086`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` bookmarking and favorites |
| `notification-service` | `/api/v1/notifications/**` | `lb://notification-service` (`:8087`) | Eureka Load Balanced | 5s / 3s | `ROLE_STUDENT` reminder preferences & logs |
| `ai-service` | `/api/v1/assistant/**` | `http://localhost:8000` | Direct HTTP / Fast-API | 15s / 5s | `ROLE_STUDENT` source-grounded scholarship Q&A |
| `ingestion-service` | `/api/v1/ingestion/**` | `http://localhost:8001` | Direct HTTP / Python | 10s / 3s | `ROLE_ADMIN` scraper trigger & ingestion monitoring |

> **Note on Path Preservation:** All routes preserve the original downstream paths directly without rewrites, matching the exact controller mappings implemented across all microservices.

---

## 3. Security Boundaries & Filter Ordering

Spring WebFlux processes requests through the following ordered filters:

```text
Incoming HTTP Request (Port 8080)
   │
   ▼
[1] RequestHeaderSanitizationFilter (Order: HIGHEST_PRECEDENCE + 1)
   - Strips all incoming 'X-User-*', 'X-Student-*', and 'X-Internal-Service-Key'
   - Generates or preserves 'X-Request-Id'
   │
   ▼
[2] InternalPathBlockFilter (Order: HIGHEST_PRECEDENCE + 2)
   - Checks if path contains '/internal/'
   - Immediately returns HTTP 403 Forbidden with RFC 7807 problem details
   │
   ▼
[3] CorsWebFilter
   - Validates Origin against allowed list (e.g., http://localhost:3000)
   - Emits Access-Control-* headers; handles preflight OPTIONS
   │
   ▼
[4] Spring Cloud Gateway Route Locator & Predicate Matching
   - Matches route based on explicit path prefixes
   │
   ▼
[5] Downstream Dispatch via Netty HTTP Client (with bounded timeouts)
   - Connect Timeout: 3s (5s AI)
   - Response Timeout: 5s (15s AI)
   │
   ▼
[6] GlobalGatewayExceptionHandler (Order: HIGHEST_PRECEDENCE)
   - Translates ConnectException -> HTTP 503 (ERR_SERVICE_UNAVAILABLE)
   - Translates TimeoutException -> HTTP 504 (ERR_GATEWAY_TIMEOUT)
   - Translates NotFoundException -> HTTP 404 (ERR_ROUTE_NOT_FOUND)
```

---

## 4. Configuration & Environment Variables

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8080` | Port for API Gateway |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka Discovery Server registry URL |
| `EUREKA_CLIENT_ENABLED` | `true` | Enable/disable Eureka client registration |
| `JWT_SECRET` | *(Standard dev key)* | 256-bit HMAC secret for token verification |
| `INTERNAL_SERVICE_KEY` | *(Standard dev key)* | Shared secret for inter-service verification |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-delimited list of allowed CORS origins |
| `AUTH_SERVICE_URL` | `lb://auth-service` | URL or load-balancer URI for Auth Service |
| `STUDENT_SERVICE_URL` | `lb://student-service` | URL or load-balancer URI for Student Service |
| `SCHOLARSHIP_SERVICE_URL` | `lb://scholarship-service` | URL or load-balancer URI for Scholarship Service |
| `ELIGIBILITY_SERVICE_URL` | `lb://eligibility-service` | URL or load-balancer URI for Eligibility Service |
| `RECOMMENDATION_SERVICE_URL` | `lb://recommendation-service` | URL or load-balancer URI for Recommendation Service |
| `APPLICATION_SERVICE_URL` | `lb://application-service` | URL or load-balancer URI for Application Service |
| `NOTIFICATION_SERVICE_URL` | `lb://notification-service` | URL or load-balancer URI for Notification Service |
| `AI_SERVICE_URL` | `http://localhost:8000` | Direct URL for AI Service (FastAPI) |
| `INGESTION_SERVICE_URL` | `http://localhost:8001` | Direct URL for Scholarship Ingestion Service |

---

## 5. Local Startup Order & Integration Workflow

To safely run PrestIQ locally on a resource-bounded machine (e.g., 8 GB MacBook Air):

### Step 1: Start Discovery Server
```bash
cd services/discovery-server
./mvnw spring-boot:run
# Wait until port 8761 is up: curl http://localhost:8761/actuator/health
```

### Step 2: Start API Gateway
```bash
cd services/api-gateway
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx256m"
# Verify gateway health: curl http://localhost:8080/actuator/health
```

### Step 3: Start Target Downstream Service (One at a time)
For example, to test the Auth or Scholarship flow:
```bash
cd services/auth-service
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xmx256m"

# Test registration through the Gateway:
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123!","firstName":"Jane","lastName":"Doe"}'
```

---

## 6. Testing & Verification

Run the full gateway automated test suite:
```bash
cd services/api-gateway
./mvnw clean test
```

### Test Suite Summary:
- **`RouteMatchingIntegrationTest`:** Verifies all 12 explicit service routes are properly loaded into Spring Cloud Gateway, tests Actuator health endpoint (`/actuator/health`), and asserts HTTP 403 problem details when calling internal paths (`/internal/**`).
- **`CorsConfigTest`:** Verifies preflight OPTIONS approval for allowed origins (`http://localhost:3000`) with credentials, and rejection of unauthorized origins.
- **`JwtAuthenticationGatewayFilterFactoryTest`:** Verifies JWT token validation, claim extraction, role enforcement, and token expiry handling.
- **`RequestHeaderSanitizationFilterTest`:** Verifies case-insensitive stripping of untrusted `X-User-*`, `X-Student-*`, and `X-Internal-Service-Key` headers while preserving `Authorization` and adding `X-Request-Id`.
- **`InternalPathBlockFilterTest`:** Verifies perimeter isolation of internal service paths.
- **`GlobalGatewayExceptionHandlerTest`:** Verifies translation of downstream network errors into RFC 7807 problem details.

---

## 7. Known Integration Gaps & Future Work
1. **Frontend Client Integration:** Milestone 13 will integrate the React 19 SPA with this Gateway.
2. **Distributed Redis Rate Limiter:** Current gateway relies on bounded connection pools; Redis-backed Bucket4j rate limiting will be enabled when multi-replica cloud deployment is provisioned.
3. **Dynamic Token Revocation Cache:** Gateway currently validates JWT signatures and expirations stateless; real-time blacklist checks will connect to Redis when shared cache infrastructure is introduced.
