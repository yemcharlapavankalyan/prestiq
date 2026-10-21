"""Tests for source registry validation, configuration, and CRUD."""

import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_source_registry_validation_errors(client: AsyncClient, admin_jwt_token: str):
    headers = {"Authorization": f"Bearer {admin_jwt_token}"}

    # Invalid slug (uppercase & spaces)
    resp = await client.post(
        "/api/v1/ingestion/sources",
        headers=headers,
        json={
            "id": "INVALID SLUG!",
            "name": "Invalid Source",
            "base_url": "https://valid.gov",
            "category": "GOVERNMENT",
            "allowed_domains": ["valid.gov"],
        },
    )
    assert resp.status_code == 422

    # Missing allowed domains
    resp2 = await client.post(
        "/api/v1/ingestion/sources",
        headers=headers,
        json={
            "id": "missing-domains",
            "name": "Missing Domains Source",
            "base_url": "https://valid.gov",
            "category": "GOVERNMENT",
            "allowed_domains": [],
        },
    )
    assert resp2.status_code == 422


@pytest.mark.asyncio
async def test_source_crud_operations(client: AsyncClient, admin_jwt_token: str, student_jwt_token: str):
    admin_headers = {"Authorization": f"Bearer {admin_jwt_token}"}
    student_headers = {"Authorization": f"Bearer {student_jwt_token}"}

    # 1. Student role cannot create source
    resp_forbidden = await client.post(
        "/api/v1/ingestion/sources",
        headers=student_headers,
        json={
            "id": "student-created",
            "name": "Student Created",
            "base_url": "https://test.gov",
            "category": "GOVERNMENT",
            "allowed_domains": ["test.gov"],
        },
    )
    assert resp_forbidden.status_code == 403

    # 2. Admin creates source
    create_resp = await client.post(
        "/api/v1/ingestion/sources",
        headers=admin_headers,
        json={
            "id": "nasa-fellowships",
            "name": "NASA Fellowship Program",
            "base_url": "https://nasa.gov/fellowships",
            "category": "GOVERNMENT",
            "allowed_domains": ["nasa.gov"],
            "refresh_interval_minutes": 720,
            "rate_limit_rpm": 20,
        },
    )
    assert create_resp.status_code == 201
    data = create_resp.json()
    assert data["id"] == "nasa-fellowships"
    assert data["allowed_domains"] == ["nasa.gov"]

    # 3. List sources
    list_resp = await client.get("/api/v1/ingestion/sources")
    assert list_resp.status_code == 200
    all_sources = list_resp.json()
    assert any(s["id"] == "nasa-fellowships" for s in all_sources)

    # 4. Get specific source
    get_resp = await client.get("/api/v1/ingestion/sources/nasa-fellowships")
    assert get_resp.status_code == 200
    assert get_resp.json()["name"] == "NASA Fellowship Program"

    # 5. Patch source
    patch_resp = await client.patch(
        "/api/v1/ingestion/sources/nasa-fellowships",
        headers=admin_headers,
        json={"rate_limit_rpm": 25, "enabled": False},
    )
    assert patch_resp.status_code == 200
    assert patch_resp.json()["rate_limit_rpm"] == 25
    assert patch_resp.json()["enabled"] is False

    # 6. Delete source
    del_resp = await client.delete("/api/v1/ingestion/sources/nasa-fellowships", headers=admin_headers)
    assert del_resp.status_code == 204

    # 7. Verify deletion
    get_after_del = await client.get("/api/v1/ingestion/sources/nasa-fellowships")
    assert get_after_del.status_code == 404
