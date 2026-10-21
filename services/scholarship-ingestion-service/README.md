# PrestIQ Scholarship Ingestion Service

**Service Port:** `8001`  
**Runtime:** Python 3.13 / FastAPI  
**Database:** `prestiq_ingestion_db` (PostgreSQL 16) / SQLite for local testing  
**Architecture Specification:** [docs/architecture/scholarship-ingestion.md](../../docs/architecture/scholarship-ingestion.md)

---

## 1. Overview & Service Scope

The `scholarship-ingestion-service` is an independent, source-driven pipeline microservice designed to discover, fetch, extract, normalize, validate, deduplicate, and submit scholarship opportunities for Human-in-the-Loop (HITL) review.

### Core Principles
1. **Quality & Traceability over Volume:** Every candidate record preserves full provenance (source URL, retrieval timestamp, HTTP status, and cryptographic SHA-256 content hash).
2. **Strict Verification Boundaries:** Machine ingestion **never** verifies or publishes scholarships directly. Ingested candidates are submitted to `scholarship-service` in `DRAFT` status, attached with official provenance sources, and transitioned to `PENDING_REVIEW` for scholarship officers to review.
3. **SSRF & Network Defense:** Outbound requests are strictly restricted to authorized domain whitelists. Private IP ranges, loopback destinations, link-local metadata addresses, and embedded URL credentials are blocked.
4. **Untrusted Data Isolation:** Web page content is treated as untrusted text. Prompt injection attempts cannot bypass system rules, alter verification status, or execute administrative commands.
5. **Polite Crawling & Bounded Resources:** Complies with robots policies, crawl delays, sliding-window rate limits, 5 MB payload size caps, and exponential backoff for transient HTTP 429/5xx responses.

---

## 2. Ingestion Pipeline Stages

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ Source Registry │ ───►  │  Polite Fetcher │ ───►  │ Clean Extractor │
└─────────────────┘       │ (SSRF & Limits) │       │(HTML & Evidence)│
                          └─────────────────┘       └────────┬────────┘
                                                             │
┌─────────────────────────┐                                  ▼
│   scholarship-service   │                         ┌─────────────────┐
│     :8083 (Drafts)      │ ◄────────────────────── │   Normalizer    │
│  (Pending Review Queue) │   (If Valid & New)      │  & Deduplicator │
└─────────────────────────┘                         └─────────────────┘
```

1. **Source Discovery & Lock Management:** Checks configured sources; acquires an in-memory execution lock to prevent overlapping runs for the same source.
2. **Polite Fetching:** Validates URL against approved domains and resolved IPs; streams up to 5 MB; applies per-source rate limits; handles 429 backoff with `Retry-After`.
3. **Snapshot Archival:** Computes SHA-256 hash and persists raw HTML/text into `scraped_snapshots`.
4. **Structured Extraction:** Strips noise (scripts, styles, navigation, footers); extracts title, provider, description, deadline, amount, education level, and GPA while recording verbatim evidence quotes.
5. **Conservative Normalization:** Standardizes dates to ISO-8601 (`YYYY-MM-DD`), currencies to ISO-4217, amounts to `Decimal`, education levels to standard enums (`HIGH_SCHOOL`, `UNDERGRADUATE`, `POSTGRADUATE`, `DOCTORAL`), and generates clean slugs. Missing or ambiguous fields remain `null`.
6. **Quality Validation:** Enforces evidence rules, detects contradictory dates (e.g., start date after deadline), validates amount bounds, and scores extraction confidence.
7. **Deduplication & Change Detection:** Normalizes canonical URLs (stripping tracking query parameters like `utm_*` and `fbclid`). Categorizes candidates as `NEW`, `EXACT_DUPLICATE` (hash match), or `CONTENT_CHANGED` (shifted deadlines or amounts).
8. **Intake Submission:** Submits valid, new opportunities to `scholarship-service` via `POST /api/v1/admin/scholarships` with `X-Internal-Service-Key`, records source provenance via `POST /api/v1/admin/scholarships/{id}/sources`, and transitions the record to `PENDING_REVIEW` via `PUT /api/v1/admin/scholarships/{id}/submit-review`. Candidates with validation errors or conflicts remain in the local review queue.

---

## 3. Source Registry & Lawful Access

Only authorized, official sources are permitted. Out-of-the-box seeds include:
- `us-gov-studentaid`: Federal Student Aid Portal (`studentaid.gov`, `ed.gov`)
- `nsf-grfp`: National Science Foundation Graduate Research Fellowship (`nsfgrfp.org`, `nsf.gov`)
- `gates-scholarship`: The Gates Scholarship Foundation (`thegatesscholarship.org`)

### Access Rules
- No scraping of sites that prohibit automated access in published Terms of Use.
- No bypassing of CAPTCHAs, authentication walls, paywalls, or anti-bot protections.
- Enforces user agent: `PrestiqScholarshipBot/1.0 (+https://prestiq.org/bot)`.

---

## 4. API Reference

All administrative operations require `ROLE_ADMIN` or a valid `X-Internal-Service-Key`.

### Health & Monitoring
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Standard health status | Public |
| `GET` | `/health/ready` | Traffic readiness probe | Public |
| `GET` | `/health/live` | Liveness probe | Public |
| `GET` | `/info` | Capability and runtime information | Public |

### Source Registry (`/api/v1/ingestion/sources`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/ingestion/sources` | List all configured sources | Public |
| `GET` | `/api/v1/ingestion/sources/{id}` | Get specific source configuration | Public |
| `POST` | `/api/v1/ingestion/sources` | Register a new official source | Admin / Internal |
| `PATCH`| `/api/v1/ingestion/sources/{id}` | Update existing source settings | Admin / Internal |
| `DELETE`| `/api/v1/ingestion/sources/{id}` | Delete source | Admin / Internal |
| `POST` | `/api/v1/ingestion/sources/{id}/trigger` | Trigger immediate ingestion run | Admin / Internal |

### Ingestion Runs (`/api/v1/ingestion/runs`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/ingestion/runs` | List runs (filter by `source_id`, `status`) | Public |
| `GET` | `/api/v1/ingestion/runs/{run_id}` | Detailed run metrics and error messages | Public |
| `POST` | `/api/v1/ingestion/runs/{run_id}/retry` | Retry a failed run | Admin / Internal |

### Candidates & Review Queue (`/api/v1/ingestion`)
| Method | Path | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/ingestion/candidates` | List candidates (filter by status, source) | Public |
| `GET` | `/api/v1/ingestion/candidates/{id}` | Candidate detail, evidence, and validation errors | Public |
| `POST`| `/api/v1/ingestion/candidates/{id}/submit` | Manually submit reviewed candidate | Admin / Internal |
| `GET` | `/api/v1/ingestion/conflicts` | List candidates with detected content changes | Public |

---

## 5. Security & SSRF Controls

1. **Allowed Domain Whitelisting:** Every source maintains a strict whitelist of permitted hostnames.
2. **DNS & IP Validation:** Resolves destination hostnames and blocks connections to:
   - Loopback (`127.0.0.0/8`, `::1`)
   - Private RFC 1918 networks (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`)
   - Link-local and cloud metadata endpoints (`169.254.0.0/16`, `fe80::/10`)
   - Multicast and reserved ranges (`224.0.0.0/4`, `240.0.0.0/4`, `0.0.0.0/8`)
3. **Redirect Protection:** All HTTP redirects are inspected and must pass SSRF and domain whitelist checks at every hop.
4. **Credential Stripping:** URLs containing embedded userinfo (`user:pass@host`) are rejected immediately.
5. **Anti-Tampering:** Direct injection of `X-User-Id` or `X-User-Roles` headers is rejected unless accompanied by a verified `X-Internal-Service-Key`.

---

## 6. Testing & Verification

Run the automated test suite:
```bash
cd services/scholarship-ingestion-service
./.venv/bin/pytest -v
```

### Test Suite Coverage (35 Tests)
- Source registry validation, CRUD, and official configuration.
- SSRF prevention, IP resolution blocking, redirect validation, and credential rejection.
- Polite fetching, streaming size limiter (5 MB), 429 Retry-After, and permanent error handling.
- HTML extraction, script stripping, and evidence tracking.
- Conservative normalization of dates, amounts, currencies, and slugs.
- Candidate validation, confidence calculation, and contradictory dates detection.
- Canonical URL deduplication, cryptographic SHA-256 hashing, and shifted deadline detection.
- LLM schema validation, prompt injection defense, and rule-based fallback.
- Scholarship service draft submission, source attachment, and verification boundaries.
- Run locking to prevent overlapping executions for the same source.
- Scheduler freshness detection using an injectable clock.
- Candidates review queue and conflict reporting.

---

## 7. Known Limitations & Operational Notes

1. **PDF Guideline Parsing:** The current service emphasizes clean HTML extraction (`BeautifulSoup`) and JSON feeds. Complex multi-page PDF eligibility matrices require offline conversion or future document processing workers (`pdfplumber`).
2. **Local Development Persistence:** Fast unit testing uses in-memory SQLite with `aiosqlite`. Production deployments connect to PostgreSQL 16 (`prestiq_ingestion_db`) via `asyncpg`.
3. **Live Upstream External Calls:** Standard automated test suites run entirely against mocked HTTP fixtures and local endpoints to ensure deterministic execution without third-party network dependencies.
