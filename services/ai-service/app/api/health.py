"""Health, readiness, liveness, and service metadata endpoints."""

from fastapi import APIRouter
from app.config import settings
from app.models.schemas import HealthResponse
from app.providers.factory import provider_factory

router = APIRouter(tags=["Health & Status"])


@router.get("/health", response_model=HealthResponse)
async def get_health():
    """Overall health check including active provider and database status."""
    active_provider = settings.primary_ai_provider if not settings.use_mock_providers else "mock"
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
        activeProvider=active_provider,
        fallbackProvider=settings.fallback_ai_provider,
        database="prestiq_ai_db (in-memory / pgvector ready)"
    )


@router.get("/health/ready")
async def get_readiness():
    """Readiness probe indicating service is ready to handle traffic."""
    return {"status": "READY", "service": settings.service_name}


@router.get("/health/live")
async def get_liveness():
    """Liveness probe indicating service process is running."""
    return {"status": "LIVE", "service": settings.service_name}


@router.get("/info")
async def get_info():
    """Public service metadata and provider configuration."""
    chain = provider_factory.get_fallback_chain()
    return {
        "service": settings.service_name,
        "version": settings.service_version,
        "environment": settings.env,
        "fallbackChain": chain,
        "primaryProvider": settings.primary_ai_provider,
        "geminiModel": settings.gemini_model,
        "groqModel": settings.groq_model,
        "cerebrasModel": settings.cerebras_model,
        "openrouterModel": settings.openrouter_model,
        "embeddingDimensions": settings.embedding_dimensions,
        "maxQuestionLength": settings.max_question_length
    }
