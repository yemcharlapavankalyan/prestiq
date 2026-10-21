"""Security utilities for stateless JWT token validation and inter-service authentication."""

import secrets
from typing import Optional
from uuid import UUID
import jwt
from app.config import settings
from app.core.exceptions import AuthenticationError, AuthorizationError


class UserPrincipal:
    """Authenticated user context derived from validated JWT token or trusted internal gateway."""

    def __init__(
        self,
        user_id: Optional[UUID],
        email: Optional[str],
        roles: list[str],
        is_internal: bool = False
    ):
        self.user_id = user_id
        self.email = email
        self.roles = roles
        self.is_internal = is_internal

    def has_role(self, role: str) -> bool:
        target = role if role.startswith("ROLE_") else f"ROLE_{role}"
        return target in self.roles


def decode_jwt_token(token: str) -> dict:
    """Validate and decode HMAC-SHA256 Bearer JWT token."""
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=[settings.jwt_algorithm],
            issuer=settings.jwt_issuer,
            options={"require": ["exp", "iss", "sub"]}
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise AuthenticationError("JWT token has expired")
    except jwt.InvalidTokenError as e:
        raise AuthenticationError(f"Invalid JWT token: {str(e)}")


def verify_internal_service_key(key: Optional[str]) -> bool:
    """Constant-time comparison against configured internal service key."""
    if not key or not settings.internal_service_key:
        return False
    return secrets.compare_digest(key, settings.internal_service_key)


def authenticate_request(
    authorization_header: Optional[str] = None,
    internal_key_header: Optional[str] = None,
    x_user_id: Optional[str] = None,
    x_user_email: Optional[str] = None,
    x_user_roles: Optional[str] = None,
) -> UserPrincipal:
    """
    Authenticate incoming request.
    Strict Anti-Tampering Rule:
    Never trust X-User-* headers unless accompanied by verified X-Internal-Service-Key.
    """
    # 1. Check Bearer Token
    if authorization_header and authorization_header.startswith("Bearer "):
        token = authorization_header[7:].strip()
        payload = decode_jwt_token(token)
        user_id_str = payload.get("sub")
        user_id = UUID(user_id_str) if user_id_str else None
        email = payload.get("email")
        roles = payload.get("roles", [])
        return UserPrincipal(user_id=user_id, email=email, roles=roles, is_internal=False)

    # 2. Check Trusted Internal Gateway
    if internal_key_header and verify_internal_service_key(internal_key_header):
        user_id = None
        if x_user_id:
            try:
                user_id = UUID(x_user_id)
            except ValueError:
                pass
        roles = [r.strip() for r in x_user_roles.split(",")] if x_user_roles else ["ROLE_INTERNAL"]
        return UserPrincipal(user_id=user_id, email=x_user_email, roles=roles, is_internal=True)

    raise AuthenticationError("Missing or invalid authentication credentials")
