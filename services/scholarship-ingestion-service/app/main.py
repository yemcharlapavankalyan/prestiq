"""FastAPI Application factory for PrestIQ Scholarship Ingestion Service."""

from contextlib import asynccontextmanager
import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api import candidates, health, runs, sources
from app.config import settings
from app.core.exceptions import register_exception_handlers
from app.models.db_models import SourceConfig
from app.models.enums import FetchStrategy, SourceCategory
from app.scheduler.runner import scheduler
from app.storage.database import get_session_factory, init_db
from app.storage.repository import IngestionRepository

logging.basicConfig(
    level=logging.INFO if not settings.debug else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("prestiq.ingestion")


async def seed_official_sources_if_empty() -> None:
    """Populates authoritative public sources if registry is empty."""
    session_factory = get_session_factory()
    async with session_factory() as session:
        repo = IngestionRepository(session)
        existing = await repo.list_sources()
        if not existing:
            official_sources = [
                SourceConfig(
                    id="us-gov-studentaid",
                    name="Federal Student Aid Portal",
                    base_url="https://studentaid.gov/understand-aid/types/scholarships",
                    category=SourceCategory.GOVERNMENT.value,
                    enabled=True,
                    fetch_strategy=FetchStrategy.STATIC_HTML.value,
                    refresh_interval_minutes=1440,
                    rate_limit_rpm=15,
                    request_timeout_seconds=15.0,
                    terms_of_use="Public government domain; polite automated crawling with robots.txt compliance.",
                ),
                SourceConfig(
                    id="nsf-grfp",
                    name="National Science Foundation Graduate Research Fellowship Program",
                    base_url="https://www.nsfgrfp.org/applicants/eligibility/",
                    category=SourceCategory.GOVERNMENT.value,
                    enabled=True,
                    fetch_strategy=FetchStrategy.STATIC_HTML.value,
                    refresh_interval_minutes=1440,
                    rate_limit_rpm=15,
                    request_timeout_seconds=15.0,
                    terms_of_use="Official government fellowship guidelines; public access.",
                ),
                SourceConfig(
                    id="gates-scholarship",
                    name="The Gates Scholarship Foundation",
                    base_url="https://www.thegatesscholarship.org/scholarship",
                    category=SourceCategory.PRIVATE_FOUNDATION.value,
                    enabled=True,
                    fetch_strategy=FetchStrategy.STATIC_HTML.value,
                    refresh_interval_minutes=1440,
                    rate_limit_rpm=10,
                    request_timeout_seconds=15.0,
                    terms_of_use="Official accredited foundation portal.",
                ),
            ]
            official_sources[0].allowed_domains = ["studentaid.gov", "ed.gov"]
            official_sources[1].allowed_domains = ["nsfgrfp.org", "nsf.gov"]
            official_sources[2].allowed_domains = ["thegatesscholarship.org"]

            for src in official_sources:
                await repo.create_source(src)
            await session.commit()
            logger.info("Seeded %d official authoritative scholarship sources.", len(official_sources))


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown events."""
    logger.info("Starting %s v%s on port %d [env: %s]", settings.service_name, settings.service_version, settings.port, settings.env)
    await init_db()
    await seed_official_sources_if_empty()

    if settings.scheduler_enabled:
        await scheduler.start()

    yield

    if settings.scheduler_enabled:
        await scheduler.stop()
    logger.info("Shutting down %s cleanly", settings.service_name)


def create_app() -> FastAPI:
    """Create and configure FastAPI application instance."""
    app = FastAPI(
        title="PrestIQ Scholarship Ingestion Service",
        description="Source-driven ingestion pipeline that discovers, normalizes, deduplicates, and submits candidate scholarships for human review.",
        version=settings.service_version,
        lifespan=lifespan,
        docs_url="/docs",
        redoc_url="/redoc",
        openapi_url="/openapi.json",
    )

    allowed_origins = [o.strip() for o in settings.cors_allowed_origins.split(",") if o.strip()]
    app.add_middleware(
        CORSMiddleware,
        allow_origins=allowed_origins,
        allow_credentials=True,
        allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"],
        allow_headers=["Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-Request-Id", "X-Internal-Service-Key"],
    )

    register_exception_handlers(app)

    app.include_router(health.router)
    app.include_router(sources.router)
    app.include_router(runs.router)
    app.include_router(candidates.router)

    return app


app = create_app()
