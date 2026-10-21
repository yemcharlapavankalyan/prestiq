"""Tests for health, liveness, and info endpoints."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_endpoints(client: AsyncClient):
    resp = await client.get("/health")
    assert resp.status_code == 200
    data = resp.json()
    assert data["status"] == "UP"
    assert data["service"] == "scholarship-ingestion-service"

    ready_resp = await client.get("/health/ready")
    assert ready_resp.status_code == 200
    assert ready_resp.json()["status"] == "UP"

    live_resp = await client.get("/health/live")
    assert live_resp.status_code == 200
    assert live_resp.json()["status"] == "UP"

    info_resp = await client.get("/info")
    assert info_resp.status_code == 200
    info_data = info_resp.json()
    assert info_data["service"] == "scholarship-ingestion-service"
    assert "rate_limit_default_rpm" in info_data
