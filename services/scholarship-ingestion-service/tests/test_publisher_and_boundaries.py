"""Tests for scholarship-service publisher, verification boundaries, and failure handling."""

from decimal import Decimal
import httpx
import pytest
from app.core.exceptions import UpstreamFetchError
from app.models.schemas import NormalizedScholarshipData
from app.pipeline.publisher import ScholarshipServicePublisher


SAMPLE_NORMALIZED = NormalizedScholarshipData(
    title="National Cyber Scholarship",
    slug="national-cyber-scholarship",
    provider_name="Cyber Foundation",
    description="Full tuition cybersecurity award for undergraduate computer science students.",
    official_website_url="https://cyber.org/awards",
    application_deadline="2027-11-30",
    award_amount_min=Decimal("15000.00"),
    award_amount_max=Decimal("15000.00"),
    currency_code="USD",
)


@pytest.mark.asyncio
async def test_publisher_submits_draft_attaches_source_and_advances_to_pending_review():
    calls = []

    def mock_handler(request: httpx.Request) -> httpx.Response:
        calls.append((request.method, str(request.url.path)))

        # Verify internal service key and admin roles
        assert request.headers.get("X-Internal-Service-Key") == "prestiq-internal-dev-secret-key-2026"
        assert "ROLE_ADMIN" in request.headers.get("X-User-Roles", "")

        if request.method == "POST" and request.url.path == "/api/v1/admin/scholarships":
            return httpx.Response(201, json={"id": 505, "title": "National Cyber Scholarship"})
        elif request.method == "POST" and request.url.path == "/api/v1/admin/scholarships/505/sources":
            return httpx.Response(201, json={"id": 1, "scholarshipId": 505})
        elif request.method == "PUT" and request.url.path == "/api/v1/admin/scholarships/505/submit-review":
            return httpx.Response(200, json={"id": 505, "verificationStatus": "PENDING_REVIEW"})

        return httpx.Response(404)

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(mock_handler))
    publisher = ScholarshipServicePublisher(base_url="http://mock-scholarship-svc", client=mock_client)

    schol_id = await publisher.submit_draft_for_review(
        normalized=SAMPLE_NORMALIZED,
        source_url="https://cyber.org/awards",
        content_hash="64_char_sha256_hash_here_1234567890123456789012345678901234567890",
    )

    assert schol_id == 505
    # Verify the 3-step sequence was followed:
    assert len(calls) == 3
    assert calls[0] == ("POST", "/api/v1/admin/scholarships")
    assert calls[1] == ("POST", "/api/v1/admin/scholarships/505/sources")
    assert calls[2] == ("PUT", "/api/v1/admin/scholarships/505/submit-review")

    # Invariant: NEVER calls verify or publish!
    assert not any("/verify" in path for _, path in calls)
    assert not any("/publish" in path for _, path in calls)


@pytest.mark.asyncio
async def test_publisher_handles_scholarship_service_api_failure():
    def mock_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(500, json={"detail": "Database connection error in scholarship service"})

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(mock_handler))
    publisher = ScholarshipServicePublisher(base_url="http://mock-scholarship-svc", client=mock_client)

    with pytest.raises(UpstreamFetchError, match="scholarship-service rejected draft creation"):
        await publisher.submit_draft_for_review(
            normalized=SAMPLE_NORMALIZED,
            source_url="https://cyber.org/awards",
            content_hash="64_char_sha256_hash_here_1234567890123456789012345678901234567890",
        )
