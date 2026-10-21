"""Validation rules for extracted scholarship candidates."""

from datetime import datetime
from decimal import Decimal
from typing import List, Tuple
from urllib.parse import urlsplit
from app.models.enums import CandidateValidationStatus
from app.models.schemas import ExtractedScholarshipData, NormalizedScholarshipData


class CandidateValidator:
    """Enforces rigorous syntactic and semantic validation on normalized candidates."""

    def validate(
        self,
        extracted: ExtractedScholarshipData,
        normalized: NormalizedScholarshipData,
    ) -> Tuple[CandidateValidationStatus, List[str], float]:
        """
        Validates the candidate:
        - Mandatory evidence checks
        - Date consistency
        - Amount constraints
        - Returns (status, errors, confidence_score)
        """
        errors: List[str] = []
        confidence: float = 1.0

        # 1. Mandatory Field Evidence
        if not normalized.title or len(normalized.title.strip()) < 3:
            errors.append("Title is missing or too short (minimum 3 characters required).")
            confidence -= 0.3
        elif "title" not in extracted.evidence_snippets:
            confidence -= 0.1

        if not normalized.provider_name or len(normalized.provider_name.strip()) < 2:
            errors.append("Provider name is missing or too short.")
            confidence -= 0.3

        if not normalized.description or len(normalized.description.strip()) < 20:
            errors.append("Description is missing or too brief (minimum 20 characters required).")
            confidence -= 0.2

        # 2. URL Validation
        if not normalized.official_website_url:
            errors.append("Official website URL is missing.")
            confidence -= 0.3
        else:
            try:
                parts = urlsplit(normalized.official_website_url)
                if parts.scheme.lower() not in ("http", "https") or not parts.hostname:
                    errors.append(f"Invalid official website URL: {normalized.official_website_url}")
                    confidence -= 0.2
            except Exception:
                errors.append(f"Malformed official website URL: {normalized.official_website_url}")
                confidence -= 0.2

        # 3. Application Deadline Validation
        if not normalized.application_deadline:
            errors.append("Application deadline is missing.")
            confidence -= 0.4
        else:
            try:
                deadline_dt = datetime.strptime(normalized.application_deadline, "%Y-%m-%d").date()
                if deadline_dt < datetime.now().date():
                    errors.append(f"Application deadline '{normalized.application_deadline}' is in the past (expired scholarship).")
                    confidence -= 0.2
            except ValueError:
                errors.append(f"Application deadline '{normalized.application_deadline}' is not a valid ISO date.")
                confidence -= 0.3

        # 4. Contradictory Dates Check
        if normalized.application_start_date and normalized.application_deadline:
            try:
                start_dt = datetime.strptime(normalized.application_start_date, "%Y-%m-%d").date()
                deadline_dt = datetime.strptime(normalized.application_deadline, "%Y-%m-%d").date()
                if start_dt > deadline_dt:
                    errors.append(
                        f"Contradictory dates: start date '{normalized.application_start_date}' is after deadline '{normalized.application_deadline}'."
                    )
                    confidence -= 0.4
            except ValueError:
                pass

        # 5. Award Amount Consistency
        if normalized.award_amount_min is not None and normalized.award_amount_max is not None:
            if normalized.award_amount_min > normalized.award_amount_max:
                errors.append(
                    f"Contradictory award amounts: min {normalized.award_amount_min} exceeds max {normalized.award_amount_max}."
                )
                confidence -= 0.3

        # 6. GPA Scale Consistency
        if normalized.min_gpa is not None:
            if normalized.min_gpa < Decimal("0.0") or normalized.min_gpa > normalized.gpa_scale:
                errors.append(
                    f"Minimum GPA {normalized.min_gpa} is out of bounds for scale {normalized.gpa_scale}."
                )
                confidence -= 0.2

        # Final Status determination
        confidence = max(0.0, min(1.0, confidence))
        if any("Contradictory" in e for e in errors):
            status = CandidateValidationStatus.CONFLICT
        elif errors:
            status = CandidateValidationStatus.INVALID
        else:
            status = CandidateValidationStatus.VALID

        return status, errors, round(confidence, 2)
