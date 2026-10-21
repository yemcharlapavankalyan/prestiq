"""Health and monitoring endpoints for Ingestion Service."""

from datetime import datetime, timezone
from fastapi import APIRouter
from app.config import settings
from app.models.schemas import HealthResponse

router = APIRouter(tags=["Health & Status"])


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """Standard health check endpoint."""
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
        database="UP",
        timestamp=datetime.now(timezone.utc),
    )


@router.get("/health/ready", response_model=HealthResponse)
async def readiness_check():
    """Readiness probe for traffic routing."""
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
        database="UP",
        timestamp=datetime.now(timezone.utc),
    )


@router.get("/health/live", response_model=HealthResponse)
async def liveness_check():
    """Liveness probe for process orchestrators."""
    return HealthResponse(
        status="UP",
        service=settings.service_name,
        version=settings.service_version,
        database="UP",
        timestamp=datetime.now(timezone.utc),
    )


@router.get("/info")
async def info():
    """Operational capability and configuration information."""
    return {
        "service": settings.service_name,
        "version": settings.service_version,
        "environment": settings.env,
        "port": settings.port,
        "llm_extraction_enabled": settings.llm_extraction_enabled,
        "llm_provider": settings.llm_provider,
        "rate_limit_default_rpm": settings.rate_limit_default_rpm,
        "max_response_bytes": settings.max_response_bytes,
        "scheduler_enabled": settings.scheduler_enabled,
    }
