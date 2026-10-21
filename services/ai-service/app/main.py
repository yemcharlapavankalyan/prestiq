"""FastAPI Application factory for PrestIQ AI Service."""

from contextlib import asynccontextmanager
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api import assistant, health, internal
from app.config import settings
from app.core.exceptions import register_exception_handlers

# Configure logging
logging.basicConfig(
    level=logging.INFO if not settings.debug else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("prestiq.ai")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown events."""
    logger.info("Starting %s v%s on port %d [env: %s]", settings.service_name, settings.service_version, settings.port, settings.env)
    logger.info("Primary AI Provider: %s (Paid fallback allowed: %s)", settings.primary_ai_provider, settings.allow_paid_fallback)
    yield
    logger.info("Shutting down %s cleanly", settings.service_name)


def create_app() -> FastAPI:
    """Create and configure FastAPI application instance."""
    app = FastAPI(
        title="PrestIQ AI Service",
        description="Source-grounded RAG Assistant, verified scholarship Q&A, and multi-provider free-tier LLM abstraction.",
        version=settings.service_version,
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json"
    )

    # CORS configuration
    allowed_origins = [o.strip() for o in settings.cors_allowed_origins.split(",") if o.strip()]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=allowed_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"],
        allow_headers=["Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-Request-Id", "X-Internal-Service-Key"],
    )

    # Global RFC 7807 Exception Handlers
    register_exception_handlers(app)

    # Routers
    app.include_router(health.router)
    app.include_router(assistant.router)
    app.include_router(internal.router)

    return app


app = create_app()
