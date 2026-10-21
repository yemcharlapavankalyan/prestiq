"""Tests for canonical URL normalization, cryptographic hashing, and deduplication."""

from decimal import Decimal
import pytest
from app.models.db_models import CandidateScholarship
from app.models.enums import CandidateDeduplicationStatus
from app.models.schemas import NormalizedScholarshipData
from app.pipeline.deduplicator import Deduplicator


def test_canonicalize_url_strips_tracking_params():
    dedup = Deduplicator()

    url1 = "https://StudentAid.gov/scholarships/?utm_source=google&utm_medium=cpc#deadline"
    canonical1 = dedup.canonicalize_url(url1)
    assert canonical1 == "https://studentaid.gov/scholarships"

    url2 = "https://nsf.gov/grants/details/?fbclid=12345&id=99&ref=newsletter"
    canonical2 = dedup.canonicalize_url(url2)
    assert canonical2 == "https://nsf.gov/grants/details?id=99"

    # Trailing slash removal
    assert dedup.canonicalize_url("https://example.gov/path/") == "https://example.gov/path"


def test_deduplicator_identifies_new_opportunity():
    dedup = Deduplicator()
    norm = NormalizedScholarshipData(
        title="New Grant",
        slug="new-grant",
        provider_name="Agency",
        description="Description",
        official_website_url="https://agency.gov/new",
        application_deadline="2027-12-01",
    )

    status, conflict = dedup.check_duplicate(
        canonical_url="https://agency.gov/new",
        content_hash="abc123hash",
        normalized=norm,
        existing_candidate=None,
    )
    assert status == CandidateDeduplicationStatus.NEW
    assert conflict is None


def test_deduplicator_identifies_exact_duplicate():
    dedup = Deduplicator()
    norm = NormalizedScholarshipData(
        title="Existing Grant",
        slug="existing-grant",
        provider_name="Agency",
        description="Description",
        official_website_url="https://agency.gov/existing",
        application_deadline="2027-12-01",
    )

    existing = CandidateScholarship(
        id="cand-1",
        source_id="agency",
        source_url="https://agency.gov/existing",
        canonical_url="https://agency.gov/existing",
        content_hash="identical_hash_123",
        title="Existing Grant",
        provider_name="Agency",
    )
    existing.normalized_data = norm.model_dump()

    status, conflict = dedup.check_duplicate(
        canonical_url="https://agency.gov/existing",
        content_hash="identical_hash_123",
        normalized=norm,
        existing_candidate=existing,
    )
    assert status == CandidateDeduplicationStatus.EXACT_DUPLICATE
    assert conflict is None


def test_deduplicator_detects_shifted_deadlines():
    dedup = Deduplicator()

    previous_norm = NormalizedScholarshipData(
        title="Updated Grant",
        slug="updated-grant",
        provider_name="Agency",
        description="Description",
        official_website_url="https://agency.gov/grant",
        application_deadline="2027-05-01",
    )

    new_norm = NormalizedScholarshipData(
        title="Updated Grant",
        slug="updated-grant",
        provider_name="Agency",
        description="Description",
        official_website_url="https://agency.gov/grant",
        application_deadline="2027-06-15",  # Extended deadline!
    )

    existing = CandidateScholarship(
        id="cand-2",
        source_id="agency",
        source_url="https://agency.gov/grant",
        canonical_url="https://agency.gov/grant",
        content_hash="hash_v1",
        title="Updated Grant",
        provider_name="Agency",
    )
    existing.normalized_data = previous_norm.model_dump()

    status, conflict = dedup.check_duplicate(
        canonical_url="https://agency.gov/grant",
        content_hash="hash_v2",  # Changed content hash
        normalized=new_norm,
        existing_candidate=existing,
    )
    assert status == CandidateDeduplicationStatus.CONTENT_CHANGED
    assert "Deadline changed from '2027-05-01' to '2027-06-15'" in conflict
