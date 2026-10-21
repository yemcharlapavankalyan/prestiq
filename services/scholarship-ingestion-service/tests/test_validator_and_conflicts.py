"""Tests for candidate quality validation, contradictory dates, and conflict classification."""

from decimal import Decimal
import pytest
from app.models.enums import CandidateValidationStatus
from app.models.schemas import ExtractedScholarshipData, NormalizedScholarshipData
from app.pipeline.validator import CandidateValidator


def test_validator_accepts_well_formed_candidate():
    validator = CandidateValidator()

    extracted = ExtractedScholarshipData(
        title="Valid STEM Grant",
        provider_name="National Science Foundation",
        description="Comprehensive award for undergraduate students in accredited programs.",
        raw_deadline="2027-10-15",
        evidence_snippets={"title": "Official document", "raw_deadline": "October 15, 2027"},
    )
    normalized = NormalizedScholarshipData(
        title="Valid STEM Grant",
        slug="valid-stem-grant",
        provider_name="National Science Foundation",
        description="Comprehensive award for undergraduate students in accredited programs.",
        official_website_url="https://nsf.gov/grants",
        application_deadline="2027-10-15",
        award_amount_min=Decimal("5000.00"),
        award_amount_max=Decimal("5000.00"),
    )

    status, errors, confidence = validator.validate(extracted, normalized)
    assert status == CandidateValidationStatus.VALID
    assert len(errors) == 0
    assert confidence >= 0.9


def test_validator_detects_contradictory_dates():
    validator = CandidateValidator()

    extracted = ExtractedScholarshipData(
        title="Contradictory Dates Grant",
        provider_name="Foundation",
        description="Grant with start date after the application deadline.",
    )
    normalized = NormalizedScholarshipData(
        title="Contradictory Dates Grant",
        slug="contradictory-dates-grant",
        provider_name="Foundation",
        description="Grant with start date after the application deadline.",
        official_website_url="https://foundation.org/grant",
        application_start_date="2027-12-01",
        application_deadline="2027-06-01",  # Before start date!
    )

    status, errors, confidence = validator.validate(extracted, normalized)
    assert status == CandidateValidationStatus.CONFLICT
    assert any("Contradictory dates" in e for e in errors)


def test_validator_flags_contradictory_amounts():
    validator = CandidateValidator()

    extracted = ExtractedScholarshipData(
        title="Contradictory Amount Grant",
        provider_name="Foundation",
        description="Grant where minimum award exceeds maximum award.",
    )
    normalized = NormalizedScholarshipData(
        title="Contradictory Amount Grant",
        slug="contradictory-amount-grant",
        provider_name="Foundation",
        description="Grant where minimum award exceeds maximum award.",
        official_website_url="https://foundation.org/grant",
        application_deadline="2027-12-01",
        award_amount_min=Decimal("20000.00"),
        award_amount_max=Decimal("10000.00"),  # min > max!
    )

    status, errors, confidence = validator.validate(extracted, normalized)
    assert status == CandidateValidationStatus.CONFLICT
    assert any("Contradictory award amounts" in e for e in errors)


def test_validator_rejects_missing_mandatory_fields():
    validator = CandidateValidator()

    extracted = ExtractedScholarshipData(
        title="",
        provider_name="",
        description="Short",
    )
    normalized = NormalizedScholarshipData(
        title="",
        slug="empty",
        provider_name="",
        description="Short",
        official_website_url="",
        application_deadline="invalid-date",
    )

    status, errors, confidence = validator.validate(extracted, normalized)
    assert status == CandidateValidationStatus.INVALID
    assert len(errors) >= 3
    assert confidence < 0.5
