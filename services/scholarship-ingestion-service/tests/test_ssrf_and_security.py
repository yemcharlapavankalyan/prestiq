"""Tests for SSRF prevention, URL validation, and header anti-tampering."""

import pytest
from httpx import AsyncClient
from app.core.exceptions import SSRFSecurityError
from app.core.ssrf_guard import validate_target_url


def test_ssrf_blocks_private_and_loopback_ips():
    # Loopback
    with pytest.raises(SSRFSecurityError, match="prohibited"):
        validate_target_url("http://127.0.0.1/admin")

    with pytest.raises(SSRFSecurityError, match="prohibited"):
        validate_target_url("http://localhost:8080/internal")

    # Private RFC 1918
    with pytest.raises(SSRFSecurityError, match="prohibited"):
        validate_target_url("http://192.168.1.10/status")

    with pytest.raises(SSRFSecurityError, match="prohibited"):
        validate_target_url("http://10.0.0.5/secrets")

    # Cloud metadata link-local
    with pytest.raises(SSRFSecurityError, match="prohibited"):
        validate_target_url("http://169.254.169.254/latest/meta-data/")


def test_ssrf_blocks_credentials_in_url():
    with pytest.raises(SSRFSecurityError, match="embedded username or password"):
        validate_target_url("https://user:password@studentaid.gov/path")


def test_ssrf_blocks_unsupported_schemes():
    with pytest.raises(SSRFSecurityError, match="Prohibited scheme"):
        validate_target_url("file:///etc/passwd")

    with pytest.raises(SSRFSecurityError, match="Prohibited scheme"):
        validate_target_url("ftp://ftp.example.org/dump.sql")


def test_ssrf_enforces_domain_whitelist():
    allowed = ["studentaid.gov", "ed.gov"]

    # Permitted
    url = validate_target_url("https://studentaid.gov/scholarships", allowed_domains=allowed, resolve_dns=False)
    assert url == "https://studentaid.gov/scholarships"

    # Permitted subdomain
    url_sub = validate_target_url("https://fafsa.ed.gov/app", allowed_domains=allowed, resolve_dns=False)
    assert url_sub == "https://fafsa.ed.gov/app"

    # Forbidden domain
    with pytest.raises(SSRFSecurityError, match="not in the source's approved domain whitelist"):
        validate_target_url("https://malicious-scholarship.com/apply", allowed_domains=allowed, resolve_dns=False)


@pytest.mark.asyncio
async def test_anti_tampering_rejects_untrusted_headers(client: AsyncClient):
    # Spoofed X-User headers without internal secret key
    resp = await client.post(
        "/api/v1/ingestion/sources",
        headers={"X-User-Roles": "ROLE_ADMIN", "X-User-Id": "11111111-1111-1111-1111-111111111111"},
        json={
            "id": "untrusted-source",
            "name": "Untrusted Portal",
            "base_url": "https://example.gov",
            "category": "GOVERNMENT",
            "allowed_domains": ["example.gov"],
        },
    )
    assert resp.status_code == 401
    assert "Untrusted identity headers rejected" in resp.json()["detail"]


@pytest.mark.asyncio
async def test_internal_service_key_authenticates_admin_operations(client: AsyncClient):
    resp = await client.post(
        "/api/v1/ingestion/sources",
        headers={"X-Internal-Service-Key": "prestiq-internal-dev-secret-key-2026"},
        json={
            "id": "authorized-source",
            "name": "Authorized Agency Portal",
            "base_url": "https://agency.gov/awards",
            "category": "GOVERNMENT",
            "allowed_domains": ["agency.gov"],
        },
    )
    assert resp.status_code == 201
    assert resp.json()["id"] == "authorized-source"
