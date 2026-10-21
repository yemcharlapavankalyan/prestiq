"""Dependency injection providers for FastAPI routes."""

from typing import Optional
from fastapi import Header, Depends
from app.config import settings
from app.core.exceptions import AuthenticationError, AuthorizationError, RateLimitExceededError
from app.core.rate_limiter import rate_limiter
from app.core.security import UserPrincipal, authenticate_request, verify_internal_service_key
from app.providers.factory import provider_factory
from app.services.qa_engine import QAEngine
from app.storage.base import VectorStore
from app.storage.memory_store import InMemoryVectorStore

# Singleton vector store instance for application lifecycle
_vector_store: VectorStore = InMemoryVectorStore()


def get_vector_store() -> VectorStore:
    """Return vector store instance."""
    return _vector_store


def get_qa_engine(vector_store: VectorStore = Depends(get_vector_store)) -> QAEngine:
    """Return QA Engine instance wired with vector store and provider factory."""
    return QAEngine(vector_store=vector_store, factory=provider_factory)


async def get_current_user(
    authorization: Optional[str] = Header(None, alias="Authorization"),
    x_internal_service_key: Optional[str] = Header(None, alias="X-Internal-Service-Key"),
    x_user_id: Optional[str] = Header(None, alias="X-User-Id"),
    x_user_email: Optional[str] = Header(None, alias="X-User-Email"),
    x_user_roles: Optional[str] = Header(None, alias="X-User-Roles"),
) -> UserPrincipal:
    """Validate Bearer JWT or trusted internal header authentication."""
    principal = authenticate_request(
        authorization_header=authorization,
        internal_key_header=x_internal_service_key,
        x_user_id=x_user_id,
        x_user_email=x_user_email,
        x_user_roles=x_user_roles
    )

    # Apply per-user client rate limiting (30 req/min)
    client_key = f"client:{principal.user_id or principal.email or 'anon'}"
    is_allowed = await rate_limiter.is_allowed(
        key=client_key,
        max_requests=settings.client_rate_limit_rpm,
        window_seconds=60.0
    )
    if not is_allowed:
        raise RateLimitExceededError("Too many questions submitted. Please wait before asking again.")

    await rate_limiter.record_request(client_key, window_seconds=60.0)
    return principal


async def require_internal_service(
    x_internal_service_key: Optional[str] = Header(None, alias="X-Internal-Service-Key"),
    authorization: Optional[str] = Header(None, alias="Authorization"),
) -> UserPrincipal:
    """Require valid X-Internal-Service-Key or ADMIN role for internal endpoints."""
    # 1. Check internal key
    if x_internal_service_key and verify_internal_service_key(x_internal_service_key):
        return UserPrincipal(user_id=None, email=None, roles=["ROLE_INTERNAL"], is_internal=True)

    # 2. Check Bearer token for ADMIN role
    if authorization and authorization.startswith("Bearer "):
        principal = authenticate_request(authorization_header=authorization)
        if principal.has_role("ADMIN"):
            return principal

    raise AuthorizationError("Access denied: Internal endpoint requires verified internal service key or ADMIN role")
