"""Tests for authentication, authorization, and header anti-tampering."""

import pytest
from httpx import AsyncClient
from tests.conftest import make_jwt_token


@pytest.mark.asyncio
async def test_unauthenticated_request_rejected(client: AsyncClient):
    response = await client.post("/api/v1/assistant/chat", json={"scholarshipId": 101, "question": "Hi"})
    assert response.status_code == 401
    assert response.headers["content-type"] == "application/problem+json"
    data = response.json()
    assert data["code"] == "ERR_UNAUTHORIZED"


@pytest.mark.asyncio
async def test_expired_jwt_rejected(client: AsyncClient):
    expired_token = make_jwt_token(expired=True)
    response = await client.post(
        "/api/v1/assistant/chat",
        headers={"Authorization": f"Bearer {expired_token}"},
        json={"scholarshipId": 101, "question": "Hi"}
    )
    assert response.status_code == 401
    assert "expired" in response.json()["detail"].lower()


@pytest.mark.asyncio
async def test_untrusted_user_headers_without_internal_key_rejected(client: AsyncClient):
    # Untrusted client attempting to forge identity via headers
    response = await client.post(
        "/api/v1/assistant/chat",
        headers={
            "X-User-Id": "550e8400-e29b-41d4-a716-446655440000",
            "X-User-Roles": "ROLE_STUDENT",
        },
        json={"scholarshipId": 101, "question": "Hi"}
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_internal_endpoint_rejects_missing_key(client: AsyncClient):
    response = await client.post(
        "/internal/v1/index/scholarship",
        json={"scholarshipId": 101, "sources": []}
    )
    assert response.status_code in (401, 403)


@pytest.mark.asyncio
async def test_internal_endpoint_rejects_invalid_key(client: AsyncClient):
    response = await client.post(
        "/internal/v1/index/scholarship",
        headers={"X-Internal-Service-Key": "wrong-key"},
        json={"scholarshipId": 101, "sources": []}
    )
    assert response.status_code in (401, 403)


@pytest.mark.asyncio
async def test_internal_endpoint_accepts_valid_internal_key(client: AsyncClient, internal_key: str):
    response = await client.post(
        "/internal/v1/index/scholarship",
        headers={"X-Internal-Service-Key": internal_key},
        json={"scholarshipId": 101, "sources": []}
    )
    assert response.status_code == 202
