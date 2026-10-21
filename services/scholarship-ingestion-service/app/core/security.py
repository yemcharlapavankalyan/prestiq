"""Security, JWT Authentication, and Anti-Tampering for Ingestion Service."""

from dataclasses import dataclass
from typing import List, Optional
from fastapi import Header, HTTPException, Security, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
import jwt
from app.config import settings

bearer_scheme = HTTPBearer(auto_error=False)


@dataclass
class UserPrincipal:
    """Authenticated caller identity."""
    user_id: Optional[str]
    email: Optional[str]
    roles: List[str]
    is_internal_service: bool = False

    def has_role(self, role: str) -> bool:
        return role in self.roles or "ROLE_ADMIN" in self.roles


def decode_jwt_token(token: str) -> dict:
    """Decodes and validates a Bearer JWT token."""
    try:
        payload = jwt.decode(
            token,
            settings.jwt_secret,
            algorithms=[settings.jwt_algorithm],
            issuer=settings.jwt_issuer,
            options={"require": ["sub", "exp"]},
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication token has expired.",
            headers={"WWW-Authenticate": "Bearer"},
        )
    except jwt.InvalidTokenError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=f"Invalid authentication token: {str(e)}",
            headers={"WWW-Authenticate": "Bearer"},
        )


async def get_current_user(
    auth_header: Optional[HTTPAuthorizationCredentials] = Security(bearer_scheme),
    x_internal_service_key: Optional[str] = Header(None, alias="X-Internal-Service-Key"),
    x_user_id: Optional[str] = Header(None, alias="X-User-Id"),
    x_user_email: Optional[str] = Header(None, alias="X-User-Email"),
    x_user_roles: Optional[str] = Header(None, alias="X-User-Roles"),
) -> UserPrincipal:
    """
    Resolves caller identity with strict anti-tampering:
    - Never trust untrusted X-User-* headers unless accompanied by verified X-Internal-Service-Key.
    - Otherwise validates standard Bearer JWT.
    """
    # 1. Check for trusted internal service key
    if x_internal_service_key and x_internal_service_key == settings.internal_service_key:
        roles = [r.strip() for r in x_user_roles.split(",")] if x_user_roles else ["ROLE_ADMIN", "ROLE_INTERNAL"]
        return UserPrincipal(
            user_id=x_user_id or "00000000-0000-0000-0000-000000000000",
            email=x_user_email or "internal@prestiq.local",
            roles=roles,
            is_internal_service=True,
        )

    # Reject untrusted identity headers without internal service key
    if x_user_id or x_user_email or x_user_roles:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Untrusted identity headers rejected. Valid internal key or Bearer token required.",
        )

    # 2. Check Bearer token
    if not auth_header:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authentication credentials missing.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    claims = decode_jwt_token(auth_header.credentials)
    user_id = claims.get("sub")
    email = claims.get("email")
    roles = claims.get("roles", ["ROLE_STUDENT"])

    return UserPrincipal(
        user_id=user_id,
        email=email,
        roles=roles,
        is_internal_service=False,
    )


async def require_admin_or_officer(
    principal: UserPrincipal = Security(get_current_user),
) -> UserPrincipal:
    """Enforces that caller has admin, scholarship officer, or internal role."""
    if not (principal.has_role("ROLE_ADMIN") or principal.has_role("ROLE_SCHOLARSHIP_OFFICER") or principal.is_internal_service):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Operation requires administrative or scholarship officer privileges.",
        )
    return principal


async def verify_internal_key(
    x_internal_service_key: Optional[str] = Header(None, alias="X-Internal-Service-Key"),
) -> bool:
    """Enforces valid internal service key."""
    if not x_internal_service_key or x_internal_service_key != settings.internal_service_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or missing X-Internal-Service-Key.",
        )
    return True
