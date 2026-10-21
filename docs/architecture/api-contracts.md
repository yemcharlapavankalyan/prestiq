# Prestiq Microservices API Contracts & Interface Specifications

**Document Version:** 2.0.0  
**API Specification Standard:** OpenAPI 3.0 (REST/JSON)  
**Error Standard:** RFC 7807 (Problem Details for HTTP APIs)  
**Public Ingress Gateway:** `http://localhost:8080` (Local) / `https://api.prestiq.org` (Production)  

---

## 1. Gateway Routing & Ingress Mapping

All external client traffic routes through **`api-gateway`** (port 8080), which validates JWT tokens and dispatches requests to the appropriate downstream microservice:

```
[ Client Request ] ──► [ api-gateway :8080 ]
                               │
       ┌───────────────────────┼───────────────────────┐
       ▼                       ▼                       ▼
/api/v1/auth/**        /api/v1/students/**    /api/v1/scholarships/**
[ auth-service :8081 ] [ student-service :8082][ scholarship-service :8083 ]
       │                       │                       │
       ▼                       ▼                       ▼
/api/v1/eligibility/** /api/v1/recommend/**   /api/v1/applications/**
[ eligibility-svc :8084][ recommend-svc :8085 ][ application-svc :8086 ]
       │                       │
       ▼                       ▼
/api/v1/assistant/**   /api/v1/notifications/**
[ ai-service :8000 ]   [ notification-svc :8087]
```

---

## 2. Global Standards & Error Envelopes

### 2.1 RFC 7807 Problem Details
Every microservice implements a standard `@RestControllerAdvice` (or FastAPI exception handler) returning RFC 7807 problem details for all 4xx/5xx responses:

```json
{
  "type": "https://api.prestiq.org/errors/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "One or more request parameters failed validation criteria.",
  "instance": "/api/v1/students/me/profile",
  "code": "ERR_VALIDATION_FAILED",
  "serviceId": "student-service",
  "timestamp": "2026-10-21T10:30:00Z",
  "invalidParams": [
    {
      "field": "cumulativeGpa",
      "rejectedValue": 4.5,
      "message": "GPA must be between 0.0 and 4.0 when gpaScale is 4.0"
    }
  ]
}
```

### 2.2 Standard Ingress Headers
`api-gateway` injects authenticated identity headers downstream after verifying the JWT:
* `X-User-Id: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`
* `X-User-Email: student@university.edu`
* `X-User-Roles: ROLE_STUDENT`
* `X-Request-Id: c1f7b09e-7164-4e96-a81d-e4a8398b1e4c`

---

## 3. Public Service Endpoints

### 3.1 `auth-service` Endpoints (`/api/v1/auth`)

* **`POST /api/v1/auth/register`** (`PUBLIC`): Register a new student account.
* **`POST /api/v1/auth/login`** (`PUBLIC`): Authenticate email & password. Sets `refreshToken` HttpOnly cookie and returns short-lived JWT:
  ```json
  {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
    "expiresInSeconds": 900,
    "user": {
      "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "email": "student@university.edu",
      "roles": ["ROLE_STUDENT"]
    }
  }
  ```
* **`POST /api/v1/auth/refresh`** (`PUBLIC` with cookie): Rotates refresh token and returns new access token.
* **`POST /api/v1/auth/logout`** (`AUTHENTICATED`): Revokes current refresh token.

---

### 3.2 `student-service` Endpoints (`/api/v1/students`)

* **`GET /api/v1/students/me/profile`** (`ROLE_STUDENT`): Retrieves student profile for the user ID in `X-User-Id`.
* **`PUT /api/v1/students/me/profile`** (`ROLE_STUDENT`): Creates or updates academic details (GPA, major, income, demographics).
* **`GET /api/v1/students/{id}/profile`** (`ROLE_ADMIN`): Admin view of a student profile.

---

### 3.3 `scholarship-service` Endpoints (`/api/v1/scholarships`)

* **`GET /api/v1/scholarships`** (`PUBLIC`): Search, filter, and paginate verified scholarships.
  * Parameters: `query`, `educationLevel`, `minAmount`, `maxAmount`, `sourceType`, `page`, `size`, `sort`.
* **`GET /api/v1/scholarships/{idOrSlug}`** (`PUBLIC`): Comprehensive details, provenance, and active criteria.
* **`POST /api/v1/admin/scholarships`** (`ROLE_ADMIN`): Create draft scholarship opportunity.
* **`PUT /api/v1/admin/scholarships/{id}/verify`** (`ROLE_ADMIN`): Change status to `VERIFIED` and publish indexing event to `ai-service`.

---

### 3.4 `eligibility-service` Endpoints (`/api/v1/eligibility`)

* **`GET /api/v1/eligibility/check/{scholarshipId}`** (`ROLE_STUDENT`): Evaluates authenticated student against the active rule set.
  * Response:
  ```json
  {
    "scholarshipId": 101,
    "scholarshipTitle": "National STEM Leaders Scholarship 2026",
    "ruleSetVersion": 2,
    "verdict": "ELIGIBLE",
    "matchPercentage": 100.0,
    "criteriaBreakdown": [
      {
        "criterionCode": "MIN_GPA",
        "description": "Minimum cumulative GPA of 3.50",
        "isMandatory": true,
        "status": "PASS",
        "studentValue": 3.85,
        "requiredValue": 3.50,
        "explanation": "Your GPA of 3.85 meets or exceeds the required 3.50 threshold."
      }
    ],
    "evaluatedAt": "2026-10-21T10:45:00Z"
  }
  ```

---

### 3.5 `recommendation-service` Endpoints (`/api/v1/recommendations`)

* **`GET /api/v1/recommendations`** (`ROLE_STUDENT`): Returns personalized scholarship recommendations ranked by deterministic eligibility match scores and upcoming deadline urgency.

---

### 3.6 `application-service` Endpoints (`/api/v1/applications`)

* **`GET /api/v1/applications`** (`ROLE_STUDENT`): List applications tracked by the student.
* **`POST /api/v1/applications`** (`ROLE_STUDENT`): Start tracking a scholarship (`stage: IN_PROGRESS`).
* **`PATCH /api/v1/applications/{id}/status`** (`ROLE_STUDENT`): Update status (`SUBMITTED_EXTERNALLY`, `AWARDED`, `REJECTED`) and trigger notification recalculations.
* **`POST /api/v1/saved-scholarships/{scholarshipId}`** (`ROLE_STUDENT`): Save/bookmark a scholarship.
* **`GET /api/v1/saved-scholarships`** (`ROLE_STUDENT`): List student's saved scholarships.

---

### 3.7 `ai-service` Endpoints (`/api/v1/assistant`)

* **`POST /api/v1/assistant/chat`** (`ROLE_STUDENT`): Source-grounded RAG assistant with verified citations.
  * Request:
  ```json
  {
    "scholarshipId": 101,
    "question": "Does this scholarship cover graduate students?"
  }
  ```
  * Response:
  ```json
  {
    "answer": "No. The official guidelines state that this scholarship is strictly available to undergraduate students enrolled in accredited 4-year institutions [Source 1, Page 2].",
    "citations": [
      {
        "sourceName": "2026 Eligibility Guidelines",
        "sourceUrl": "https://stemfoundation.org/guidelines.pdf",
        "pageNumber": 2,
        "verifiedExcerpt": "Eligible applicants must be enrolled in an undergraduate degree program."
      }
    ],
    "confidenceScore": 0.96,
    "disclaimer": "Synthesized directly from verified provider documentation."
  }
  ```

---

## 4. Inter-Service Internal REST Contracts

Inter-service requests occur on private network channels or local ports and carry the header:
`X-Internal-Service-Key: ${INTERNAL_SERVICE_SECRET}`.

### 4.1 `student-service` Internal API
* **`GET /internal/v1/students/{userId}/profile`**
  * Caller: `eligibility-service`, `recommendation-service`.
  * Response: Clean JSON representation of student academic and demographic attributes.

### 4.2 `scholarship-service` Internal API
* **`GET /internal/v1/scholarships/{id}/rule-set`**
  * Caller: `eligibility-service`.
  * Response: Active rule set criteria list and version number.
* **`GET /internal/v1/scholarships/{id}/summary`**
  * Caller: `application-service`, `recommendation-service`, `ai-service`.
  * Response: Scholarship title, deadline, award amount, and official website URL.

### 4.3 `eligibility-service` Internal API
* **`POST /internal/v1/eligibility/batch-evaluate`**
  * Caller: `recommendation-service`.
  * Request: `{ studentProfile: {...}, scholarshipIds: [101, 102, 105] }`.
  * Response: List of verdicts and match scores for ranking.

### 4.4 `ai-service` Internal API
* **`POST /internal/v1/index/scholarship`**
  * Caller: `scholarship-service` (when an opportunity is verified).
  * Request:
  ```json
  {
    "scholarshipId": 101,
    "sources": [
      {
        "sourceId": 401,
        "sourceUrl": "https://stemfoundation.org/guidelines.pdf",
        "contentType": "application/pdf"
      }
    ]
  }
  ```
  * Response: `202 Accepted` (Queues background extraction, chunking, and embedding).

### 4.5 `notification-service` Internal API
* **`POST /internal/v1/notifications/deadlines`**
  * Caller: `application-service`.
  * Request: `{ applicationId: "...", studentId: "...", deadline: "2026-12-01" }`.
  * Response: `201 Created` (Schedules 30-day, 7-day, and 1-day reminder jobs).
