# Prestiq AI Service — Multi-Provider Configuration Architecture

**Service Directory:** `services/ai-service`  
**Runtime:** Python 3.13 / FastAPI  
**Vector Database:** `prestiq_ai_db` (PostgreSQL 16 with `pgvector`)  
**Status:** Phase 1 Configuration Specification (Implementation scheduled for Phase 10)  

---

## 1. Multi-Provider Design Goals

The AI Service provides a source-grounded Retrieval-Augmented Generation (RAG) assistant for students inquiring about scholarships. To operate sustainably without cloud GPU costs or subscription lock-in, the service abstracts LLM inference behind a pluggable provider interface.

### Strict Non-Negotiable Constraints
1. **Never Fall Back to Paid Models:** `ALLOW_PAID_FALLBACK=false` is strictly enforced. If free-tier quotas on all configured providers are exhausted, the service gracefully degrades to returning raw, verified document excerpts.
2. **Never Rotate Providers to Evade Restrictions:** All provider rate limits and daily quotas are respected. The system will never create duplicate accounts or cycle keys to circumvent platform terms.
3. **Embeddings Evaluated Separately from Generation:** Groq and Cerebras do not provide embedding models. Free cloud embeddings are supplied exclusively by Google Gemini (`text-embedding-004` / `gemini-embedding-001` 768d) or Hugging Face Inference API.
4. **No Hardcoded Models or Assumed Permanent Quotas:** Model IDs and rate limits are defined dynamically via environment variables so they can be adjusted without code changes.

---

## 2. Supported Candidate Providers & Configuration Schema

| Provider Key | Candidate Models (Configurable) | Free-Tier Characteristics (Subject to Verification) | Role in Prestiq |
| :--- | :--- | :--- | :--- |
| `gemini` | `GEMINI_MODEL=gemini-1.5-flash`<br>`GEMINI_EMBEDDING_MODEL=text-embedding-004` | 15 RPM, 1M TPM, 1,500 RPD (Google AI Studio)<br>*Provides free 768d dense embeddings* | **Primary RAG Generation & Embedding Provider** |
| `groq` | `GROQ_MODEL=llama-3.3-70b-versatile`<br>`GROQ_FAST_MODEL=llama-3.1-8b-instant` | 30 RPM, 1,000 RPD (70B) / 14,400 RPD (8B)<br>*Inference only (no embeddings)* | **Primary Injection Guard & Secondary RAG Fallback** |
| `cerebras` | `CEREBRAS_MODEL=llama-3.1-70b` | 30 RPM, 60k TPM, 1M tokens/day<br>*Inference only (no embeddings)* | **Tertiary Generation Fallback** |
| `openrouter` | `OPENROUTER_MODEL=meta-llama/llama-3.1-8b-instruct:free` | 20 RPM, 50 RPD ($0 lifetime balance)<br>*Inference only* | **Emergency Buffer Fallback** |

---

## 3. Configuration Parameters (via `.env`)

```bash
# Global Routing
PRIMARY_AI_PROVIDER=gemini
FALLBACK_AI_PROVIDER=groq
TERTIARY_AI_PROVIDER=cerebras
EMERGENCY_AI_PROVIDER=openrouter
ALLOW_PAID_FALLBACK=false

# Google Gemini API
GEMINI_API_KEY=your-gemini-api-key-placeholder
GEMINI_MODEL=gemini-1.5-flash
GEMINI_EMBEDDING_MODEL=text-embedding-004
GEMINI_RPM_LIMIT=15

# Groq API
GROQ_API_KEY=your-groq-api-key-placeholder
GROQ_MODEL=llama-3.3-70b-versatile
GROQ_FAST_MODEL=llama-3.1-8b-instant
GROQ_RPM_LIMIT=30

# Cerebras API
CEREBRAS_API_KEY=your-cerebras-api-key-placeholder
CEREBRAS_MODEL=llama-3.1-70b
CEREBRAS_RPM_LIMIT=30

# OpenRouter API
OPENROUTER_API_KEY=your-openrouter-api-key-placeholder
OPENROUTER_MODEL=meta-llama/llama-3.1-8b-instruct:free
OPENROUTER_RPM_LIMIT=20

# Vector Retrieval Settings
EMBEDDING_DIMENSIONS=768
RAG_MIN_SIMILARITY=0.55
RAG_MAX_CHUNKS=5
```

---

## 4. Local Development Execution (8 GB MacBook Air M2)

Running local LLMs (e.g. Ollama, PyTorch, Transformers) on an 8 GB unified memory Mac consumes 2 to 5 GB of RAM. Therefore:
* All LLM inference and embeddings run against **free cloud APIs over HTTPS**.
* The local Python process runs lightweight `httpx` and `FastAPI`, consuming **$< 70\text{ MB}$ RAM**.

### Starting and Stopping `ai-service`:
```bash
# 1. Navigate to service directory
cd services/ai-service

# 2. Activate virtual environment
source venv/bin/activate

# 3. Start development server
uvicorn app.main:app --port 8000 --reload

# 4. Stop service cleanly
Press Ctrl+C
```
