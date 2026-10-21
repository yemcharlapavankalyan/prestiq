"""Pytest fixtures and configuration for Ingestion Service tests."""

from datetime import datetime, timedelta, timezone
from typing import AsyncGenerator
import jwt
import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)
from app.config import settings
from app.main import create_app
from app.models.db_models import Base, SourceConfig
from app.models.enums import FetchStrategy, SourceCategory
from app.storage.database import get_db_session
from app.storage.repository import IngestionRepository


# Override settings for tests
settings.database_url = "sqlite+aiosqlite:///:memory:"
settings.llm_extraction_enabled = True
settings.llm_provider = "mock"
settings.scheduler_enabled = False


@pytest_asyncio.fixture(scope="function")
async def test_engine() -> AsyncGenerator[AsyncEngine, None]:
    """In-memory SQLite async engine for tests."""
    engine = create_async_engine("sqlite+aiosqlite:///:memory:", echo=False)
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    yield engine
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
    await engine.dispose()


@pytest_asyncio.fixture(scope="function")
async def test_session(test_engine: AsyncEngine) -> AsyncGenerator[AsyncSession, None]:
    """Async database session fixture."""
    session_factory = async_sessionmaker(bind=test_engine, class_=AsyncSession, expire_on_commit=False)
    async with session_factory() as session:
        yield session


@pytest_asyncio.fixture(scope="function")
async def test_repo(test_session: AsyncSession) -> IngestionRepository:
    """Repository fixture bound to test session."""
    return IngestionRepository(test_session)


@pytest_asyncio.fixture(scope="function")
async def seeded_source(test_repo: IngestionRepository) -> SourceConfig:
    """Creates a sample authorized source in DB."""
    source = SourceConfig(
        id="us-gov-stem-portal",
        name="US Government STEM Scholarship Portal",
        base_url="https://stem.gov/scholarships",
        category=SourceCategory.GOVERNMENT.value,
        enabled=True,
        fetch_strategy=FetchStrategy.STATIC_HTML.value,
        refresh_interval_minutes=1440,
        rate_limit_rpm=30,
        request_timeout_seconds=5.0,
        terms_of_use="Public government data portal.",
    )
    source.allowed_domains = ["stem.gov"]
    created = await test_repo.create_source(source)
    await test_repo.session.commit()
    return created


@pytest.fixture
def admin_jwt_token() -> str:
    """Generates a valid test admin JWT token."""
    payload = {
        "sub": "00000000-0000-0000-0000-000000000001",
        "email": "admin@prestiq.test",
        "roles": ["ROLE_ADMIN"],
        "iss": settings.jwt_issuer,
        "exp": datetime.now(timezone.utc) + timedelta(minutes=30),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


@pytest.fixture
def student_jwt_token() -> str:
    """Generates a valid student JWT token."""
    payload = {
        "sub": "00000000-0000-0000-0000-000000000002",
        "email": "student@prestiq.test",
        "roles": ["ROLE_STUDENT"],
        "iss": settings.jwt_issuer,
        "exp": datetime.now(timezone.utc) + timedelta(minutes=30),
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


@pytest_asyncio.fixture(scope="function")
async def client(test_session: AsyncSession) -> AsyncGenerator[AsyncClient, None]:
    """Test client configured with overridden DB session."""
    app = create_app()

    async def override_get_db_session():
        yield test_session

    app.dependency_overrides[get_db_session] = override_get_db_session

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
