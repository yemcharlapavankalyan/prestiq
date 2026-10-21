"""Pytest fixtures for AI Service test suite."""

import time
from typing import AsyncGenerator, List
import jwt
import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from app.config import settings
from app.core.rate_limiter import rate_limiter
from app.main import app
from app.providers.factory import provider_factory
from app.providers.mock_provider import MockProvider
from app.storage.memory_store import InMemoryVectorStore
import app.api.deps as deps

# Force test configuration
settings.env = "test"
settings.use_mock_providers = True
settings.primary_ai_provider = "mock"


@pytest.fixture(autouse=True)
def setup_test_environment():
    """Reset vector store, providers, and settings before each test."""
    deps._vector_store = InMemoryVectorStore()
    mock_p = MockProvider(name="mock")
    provider_factory.register_provider(mock_p)

    settings.use_mock_providers = True
    settings.primary_ai_provider = "mock"
    settings.fallback_ai_provider = "groq"
    settings.tertiary_ai_provider = "cerebras"
    settings.emergency_ai_provider = "openrouter"
    yield
    settings.use_mock_providers = True
    settings.primary_ai_provider = "mock"


@pytest_asyncio.fixture(autouse=True)
async def reset_rate_limits():
    """Reset rate limiter state before each async test."""
    await rate_limiter.reset()
    yield
    await rate_limiter.reset()


def make_jwt_token(
    user_id: str = "550e8400-e29b-41d4-a716-446655440000",
    email: str = "student@example.com",
    roles: List[str] = None,
    expired: bool = False
) -> str:
    """Generate a test JWT token."""
    if roles is None:
        roles = ["ROLE_STUDENT"]

    now = int(time.time())
    exp = now - 3600 if expired else now + 3600

    payload = {
        "sub": user_id,
        "email": email,
        "roles": roles,
        "iss": settings.jwt_issuer,
        "iat": now,
        "exp": exp
    }
    return jwt.encode(payload, settings.jwt_secret, algorithm=settings.jwt_algorithm)


@pytest_asyncio.fixture
async def client() -> AsyncGenerator[AsyncClient, None]:
    """Async test client for FastAPI app."""
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://testserver") as ac:
        yield ac


@pytest.fixture
def student_token() -> str:
    return make_jwt_token(roles=["ROLE_STUDENT"])


@pytest.fixture
def admin_token() -> str:
    return make_jwt_token(roles=["ROLE_ADMIN"])


@pytest.fixture
def internal_key() -> str:
    return settings.internal_service_key
