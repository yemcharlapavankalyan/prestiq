"""Tests for candidate review queue and conflict management APIs."""

import pytest
from httpx import AsyncClient
from app.models.db_models import CandidateScholarship
from app.storage.repository import IngestionRepository


@pytest.mark.asyncio
async def test_candidates_api_listing_and_detail(
    client: AsyncClient,
    test_repo: IngestionRepository,
    admin_jwt_token: str,
):
    headers = {"Authorization": f"Bearer {admin_jwt_token}"}

    # Seed sample candidate in test DB
    candidate = CandidateScholarship(
        id="cand-101",
        source_id="nsf-grfp",
        source_url="https://nsfgrfp.org/award",
        canonical_url="https://nsfgrfp.org/award",
        content_hash="64_char_hash_12345678901234567890123456789012345678901234567890123456",
        title="NSF Graduate Research Fellowship",
        provider_name="National Science Foundation",
        confidence_score=0.95,
        validation_status="VALID",
        deduplication_status="NEW",
        submission_status="NEEDS_REVIEW",
    )
    candidate.raw_data = {"title": "NSF Graduate Research Fellowship"}
    candidate.normalized_data = {
        "title": "NSF Graduate Research Fellowship",
        "slug": "nsf-graduate-research-fellowship",
        "provider_name": "National Science Foundation",
        "description": "Fellowship for graduate STEM students.",
        "official_website_url": "https://nsfgrfp.org/award",
        "application_deadline": "2027-10-25",
        "award_amount_min": 37000.0,
        "award_amount_max": 37000.0,
        "currency_code": "USD",
        "source_type": "GOVERNMENT",
    }
    candidate.validation_errors = []
    await test_repo.create_candidate(candidate)
    await test_repo.session.commit()

    # 1. List candidates
    list_resp = await client.get("/api/v1/ingestion/candidates", headers=headers)
    assert list_resp.status_code == 200
    list_data = list_resp.json()
    assert list_data["total"] >= 1
    assert any(c["id"] == "cand-101" for c in list_data["items"])

    # 2. Get candidate detail
    get_resp = await client.get("/api/v1/ingestion/candidates/cand-101", headers=headers)
    assert get_resp.status_code == 200
    detail = get_resp.json()
    assert detail["title"] == "NSF Graduate Research Fellowship"
    assert detail["confidence_score"] == 0.95

    # 3. Nonexistent candidate
    not_found_resp = await client.get("/api/v1/ingestion/candidates/nonexistent", headers=headers)
    assert not_found_resp.status_code == 404


@pytest.mark.asyncio
async def test_conflicts_api(
    client: AsyncClient,
    test_repo: IngestionRepository,
    admin_jwt_token: str,
):
    headers = {"Authorization": f"Bearer {admin_jwt_token}"}

    # Seed a candidate with content changed (conflict)
    candidate = CandidateScholarship(
        id="cand-conflict-1",
        source_id="nsf-grfp",
        source_url="https://nsfgrfp.org/deadline-shift",
        canonical_url="https://nsfgrfp.org/deadline-shift",
        content_hash="shifted_hash",
        title="Shifted Deadline Fellowship",
        provider_name="National Science Foundation",
        confidence_score=0.85,
        validation_status="VALID",
        deduplication_status="CONTENT_CHANGED",
        submission_status="NEEDS_REVIEW",
    )
    candidate.raw_data = {}
    candidate.normalized_data = {"application_deadline": "2027-12-01"}
    candidate.validation_errors = []
    await test_repo.create_candidate(candidate)
    await test_repo.session.commit()

    conflicts_resp = await client.get("/api/v1/ingestion/conflicts", headers=headers)
    assert conflicts_resp.status_code == 200
    conflicts = conflicts_resp.json()
    assert len(conflicts) >= 1
    assert any(c["candidate_id"] == "cand-conflict-1" for c in conflicts)
