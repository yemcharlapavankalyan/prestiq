"""Tests for health, readiness, liveness, and info endpoints."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_endpoint(client: AsyncClient):
    response = await client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "ai-service"
    assert "activeProvider" in data


@pytest.mark.asyncio
async def test_readiness_endpoint(client: AsyncClient):
    response = await client.get("/health/ready")
    assert response.status_code == 200
    assert response.json()["status"] == "READY"


@pytest.mark.asyncio
async def test_liveness_endpoint(client: AsyncClient):
    response = await client.get("/health/live")
    assert response.status_code == 200
    assert response.json()["status"] == "LIVE"


@pytest.mark.asyncio
async def test_info_endpoint(client: AsyncClient):
    response = await client.get("/info")
    assert response.status_code == 200
    data = response.json()
    assert data["service"] == "ai-service"
    assert "fallbackChain" in data
    assert data["embeddingDimensions"] == 768
