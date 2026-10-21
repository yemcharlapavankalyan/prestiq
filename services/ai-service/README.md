# PrestIQ AI Service (`ai-service`)

The **AI Service** provides a source-grounded Retrieval-Augmented Generation (RAG) assistant for students inquiring about verified scholarship opportunities. The service abstracts LLM inference and embeddings behind a pluggable, multi-provider free-tier architecture with automatic rate-limit monitoring, graceful excerpt degradation, and strict guardrails preventing hallucinations or fabricated facts.

---

## 1. Service Specifications

| Characteristic | Specification |
|---|---|
| **Port** | `8000` (Configurable via `PORT`) |
| **Runtime** | Python 3.13 / FastAPI |
| **Vector Database** | PostgreSQL 16+ `prestiq_ai_db` (`document_chunks` with `pgvector`) / `InMemoryVectorStore` |
| **API Documentation** | Swagger UI (`/docs`), ReDoc (`/redoc`), OpenAPI spec (`/openapi.json`) |
| **Memory Footprint** | Lightweight Python process ($< 70\text{ MB}$ RAM on 8 GB MacBook Air M2) |
| **Grounding Policy** | Source-grounded RAG with mandatory `[Source X]` citations |
| **Eligibility Boundary** | Deterministic eligibility decisions are strictly calculated by `eligibility-service` |

---

## 2. Multi-Provider Free-Tier Architecture

To operate sustainably without cloud GPU costs or subscription lock-in, the service abstracts LLM inference and embeddings across free-tier candidate providers:

```
                  ┌─────────────────────────────┐
                  │      ProviderFactory        │
                  └──────────────┬──────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│  GeminiAdapter   │    │   GroqAdapter    │    │ CerebrasAdapter  │
│(RAG + Embeddings)│    │(Guard + Fallback)│    │ (Backup Gen)     │
└──────────────────┘    └──────────────────┘    └──────────────────┘
```

### Candidate Providers & Free-Tier Limits
1. **Google Gemini API (`gemini`)**: Primary RAG generation (`gemini-1.5-flash`) and dense 768d embeddings (`text-embedding-004`). Free tier: 15 RPM, 1M TPM, 1,500 RPD.
2. **Groq Cloud API (`groq`)**: Primary injection guard (`llama-3.1-8b-instant`) and secondary RAG generation fallback (`llama-3.3-70b-versatile`). Free tier: 30 RPM, 1,000 RPD.
3. **Cerebras API (`cerebras`)**: Tertiary generation fallback (`llama-3.1-70b`). Free tier: 30 RPM, 60k TPM.
4. **OpenRouter API (`openrouter`)**: Emergency buffer fallback for `:free` open models. Free tier: 20 RPM, 50 RPD.
5. **Mock Provider (`mock`)**: Deterministic provider for automated test suites and offline execution without external API keys.

### Strict Architectural Guarantees
- **`ALLOW_PAID_FALLBACK=false`:** If all free-tier quotas are exhausted, the service **never** falls back to paid models or triggers billing.
- **Graceful Excerpt Degradation:** If all providers hit rate limits or cooldowns, the service returns verified document excerpts directly to the student with an advisory notice.
- **No API Key Cycling:** The system strictly respects platform terms and never cycles accounts or keys to evade provider limits.

---

## 3. End-to-End Grounding & Security Guardrails

1. **Anti-Hallucination & Grounding:**
   - LLMs are instructed to rely exclusively on verified documentation excerpts.
   - If excerpts lack evidence, the assistant explicitly states: *"The provided official guidelines do not specify this information. Please consult the official scholarship link for verified details."*
2. **Mandatory Citations & Source Attribution:**
   - Every factual claim references `[Source X]` or `[Source X, Page Y]`.
   - The engine validates cited sources and returns structured `Citation` objects with canonical URLs and verified excerpts.
3. **Deterministic Eligibility Boundary:**
   - If a student asks "Am I eligible?" or "Do I qualify?", the assistant responds:
     *"I can explain the requirements from the official guidelines, but official eligibility assessments are calculated deterministically by PrestIQ's Eligibility Service to ensure accuracy. Please visit the Eligibility tab to run your automated assessment."*
4. **Prompt Injection Defense:**
   - User queries are limited to 400 characters and scanned for adversarial patterns ("ignore instructions", "developer mode", etc.).
5. **Header Anti-Tampering:**
   - Inbound `X-User-Id` and `X-User-Roles` headers from public requests are ignored; identity is resolved only from validated JWT claims (`HS256`) or when accompanied by `X-Internal-Service-Key`.

---

## 4. API Endpoints

### Health & Information
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Overall health check, active provider, and database status |
| `GET` | `/health/ready` | Readiness probe |
| `GET` | `/health/live` | Liveness probe |
| `GET` | `/info` | Service metadata, fallback chain, and model configuration |

### Public Assistant API (`/api/v1/assistant`)
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/assistant/chat` | Bearer JWT | Source-grounded RAG assistant with verified citations |

**Representative Request:**
```json
{
  "scholarshipId": 101,
  "question": "Does this scholarship cover graduate students?"
}
```

**Representative Response:**
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
  "disclaimer": "Synthesized directly from verified provider documentation. Eligibility assessments are calculated deterministically by Prestiq's Eligibility Service.",
  "providerUsed": "gemini",
  "fallbackOccurred": false
}
```

### Internal Indexing API (`/internal/v1`)
Requires `X-Internal-Service-Key: ${INTERNAL_SERVICE_KEY}` or `ROLE_ADMIN`.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/internal/v1/index/scholarship` | Chunks, embeds, and indexes official scholarship documentation |
| `GET` | `/internal/v1/chunks/{scholarship_id}` | Retrieves indexed document chunks for inspection |
| `DELETE` | `/internal/v1/chunks/{scholarship_id}` | Deletes indexed chunks when an opportunity is unpublished |

---

## 5. Local Setup & Testing

### Prerequisites
- Python 3.13 (`python3 --version`)

### Setup Virtual Environment
```bash
cd services/ai-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Run Automated Tests
```bash
./.venv/bin/pytest -v
```

### Run Service Locally
```bash
source .venv/bin/activate
uvicorn app.main:app --port 8000 --reload
```

---

## 6. Verification Status & Known Limitations

- **Automated Tests:** 27 unit and integration tests passing with 100% success using `MockProvider`.
- **Live Cloud Provider Testing:** Real Google Gemini, Groq, Cerebras, and OpenRouter API keys are configurable via environment variables (`GEMINI_API_KEY`, `GROQ_API_KEY`, etc.). Automated test suites use `MockProvider` to maintain zero external network dependencies and prevent accidental quota consumption.
- **Database Storage:** Tested with `InMemoryVectorStore`. Production runs connect to PostgreSQL `prestiq_ai_db` with `pgvector` when initialized.
