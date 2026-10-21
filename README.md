# Prestiq

> **Tagline:** *Where Merit Meets Opportunity*  
> AI-powered scholarship discovery, eligibility assessment, and application management platform.

---

## 1. Project Overview & Architecture

Prestiq is an enterprise-grade platform built on a **genuine microservices architecture**, designed to bridge deserving students with verified educational funding opportunities:

1. **Deterministic Eligibility Engine:** Versioned, transparent rule evaluation producing definitive verdicts (`ELIGIBLE`, `INELIGIBLE`, `NEEDS_VERIFICATION`) with zero AI hallucination.
2. **Verified Opportunity Catalog:** A curated scholarship repository backed by official provenance, human-in-the-loop review, and continuous change detection.
3. **Source-Grounded AI Assistant:** Multi-provider RAG assistant (Google Gemini, Groq, Cerebras, OpenRouter) answering student questions with verifiable citations.
4. **Application & Deadline Management:** End-to-end tracking of application stages, required documentation, and deadline reminders.

Comprehensive architecture specifications are documented under [`docs/architecture/`](docs/architecture/):
* [`system-architecture.md`](docs/architecture/system-architecture.md): Microservices topology, communication models, and deployment assessment.
* [`database-design.md`](docs/architecture/database-design.md): Database-per-service pattern, 9 logical schemas, and `pgvector` index design.
* [`api-contracts.md`](docs/architecture/api-contracts.md): Public REST endpoints, internal contracts, and RFC 7807 error envelopes.
* [`eligibility-engine.md`](docs/architecture/eligibility-engine.md): AST rule evaluators, missing-data handling, and golden test matrices.
* [`scholarship-ingestion.md`](docs/architecture/scholarship-ingestion.md): Crawler, HTML/PDF extraction, and human review workflows.
* [`ai-service-design.md`](docs/architecture/ai-service-design.md): Multi-provider free-tier AI strategy, embeddings, and grounding guards.
* [`security-design.md`](docs/architecture/security-design.md): Gateway security, HMAC service authentication, and FERPA PII protection.
* [`testing-strategy.md`](docs/architecture/testing-strategy.md): Per-service isolated testing, WireMock stubs, and RAG evaluation.
* [`implementation-roadmap.md`](docs/architecture/implementation-roadmap.md): 13-phase implementation plan and developer execution milestones.

---

## 2. Microservices Directory Structure

```text
prestiq/
├── docs/
│   └── architecture/                     # 9 comprehensive architectural specifications
├── services/
│   ├── api-gateway/                      # Spring Cloud Gateway (Port 8080)
│   ├── discovery-server/                 # Spring Cloud Netflix Eureka (Port 8761)
│   ├── auth-service/                     # IAM, Argon2id, JWT & Refresh Tokens (Port 8081)
│   ├── student-service/                  # Student Profiles & Academic PII (Port 8082)
│   ├── scholarship-service/              # Verified Catalog, Full-Text Search (Port 8083)
│   ├── eligibility-service/              # Deterministic Rule Engine (Port 8084)
│   ├── recommendation-service/           # Personalized Matching & Scoring (Port 8085)
│   ├── application-service/              # Application Stages & Tracking (Port 8086)
│   ├── notification-service/             # Deadline Reminders & Alerts (Port 8087)
│   ├── ai-service/                       # FastAPI Multi-Provider RAG & pgvector (Port 8000)
│   └── scholarship-ingestion-service/    # Polite Scraper, PDF/HTML Extractor (Port 8001)
├── scripts/
│   └── setup-local-databases.sh          # Safe, idempotent local PostgreSQL preparation script
├── .env.example                          # Environment variable template with placeholders
├── .gitignore                            # Comprehensive ignore rules for Java, Python, macOS, IDEs
└── README.md                             # Project overview and developer onboarding guide
```

---

## 3. Technology Stack & Build Tools

* **Java Microservices (Services 1–9):**
  * **Java Runtime:** Java 21 LTS
  * **Framework:** Spring Boot 3.3.x, Spring Cloud 2023.0.x
  * **Build Tool:** **Maven with Maven Wrapper (`./mvnw`)**
  * **Build Isolation:** Each Java microservice maintains its own independent `pom.xml`.
* **Python Microservices (Services 10–11):**
  * **Python Runtime:** Python 3.13
  * **Frameworks:** FastAPI, Uvicorn, SQLAlchemy, asyncpg
  * **Environment:** Python virtual environments (`venv`) and `pip`
* **Storage Tier:**
  * **Database:** Native PostgreSQL 16+ (Homebrew)
  * **Vector Extension:** `pgvector` (enabled in `prestiq_ai_db`)

---

## 4. Low-Memory Local Development Workflow (MacBook Air M2, 8 GB RAM)

To keep memory consumption within the 8 GB budget without altering the microservices architecture:

1. **Native PostgreSQL (Zero Docker Desktop):**
   * Run PostgreSQL natively via Homebrew: `brew services start postgresql@16`.
   * Native Postgres consumes only ~35–50 MB RAM, avoiding the 2.5–4 GB memory footprint of Docker Desktop.
2. **Zero Local LLMs:**
   * All AI inference and embeddings call free-tier cloud APIs (Gemini, Groq, Cerebras) over HTTPS; local Python processes consume $< 70\text{ MB}$ RAM.
3. **Flexible JVM Memory Tuning:**
   * Configure tuned JVM options in your shell before launching any Java service:
     ```bash
     export MAVEN_OPTS="-Xms64m -Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
     ```
   * `-XX:TieredStopAtLevel=1` enables C1 compilation without C2, reducing startup time by 60% and keeping memory bounded to ~120–180 MB per service.
4. **Develop One Service at a Time:**
   * Never run all 11 services simultaneously during routine coding.
   * Develop and test the active service in isolation, stubbing downstream microservices with **WireMock**.

---

## 5. Local Setup Instructions

### Step 1: Clone Repository & Configure Environment
```bash
# Copy environment configuration template
cp .env.example .env

# Edit .env and supply your local or free-tier API credentials
```

### Step 2: Initialize the 9 Local Databases
The provided script checks your macOS Homebrew PostgreSQL installation and idempotently creates the 9 required databases without modifying or dropping any existing data:
```bash
# Review script permissions and execute
./scripts/setup-local-databases.sh
```

### Step 3: Run an Individual Microservice
```bash
# Example: Running the Auth Service (Port 8081)
cd services/auth-service
./mvnw spring-boot:run

# Example: Running the Student Service (Port 8082)
cd services/student-service
./mvnw spring-boot:run

# Example: Running the Scholarship Service (Port 8083)
cd services/scholarship-service
./mvnw spring-boot:run

# Example: Running the AI Service (Port 8000)
cd services/ai-service
source venv/bin/activate
uvicorn app.main:app --port 8000 --reload
```
To terminate any running service, press `Ctrl+C`.
