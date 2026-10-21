"""Client for submitting candidate scholarships to scholarship-service for human review."""

import logging
from typing import Optional
import httpx
from app.config import settings
from app.core.exceptions import UpstreamFetchError
from app.models.schemas import NormalizedScholarshipData

logger = logging.getLogger("prestiq.ingestion.publisher")


class ScholarshipServicePublisher:
    """
    Submits normalized candidates to scholarship-service as drafts for HITL review.
    Enforces the invariant that machine ingestion NEVER verifies or publishes directly.
    """

    def __init__(self, base_url: Optional[str] = None, client: Optional[httpx.AsyncClient] = None):
        self.base_url = (base_url or settings.scholarship_service_url).rstrip("/")
        self._client = client

    def _get_headers(self) -> dict:
        return {
            "Content-Type": "application/json",
            "X-Internal-Service-Key": settings.internal_service_key,
            "X-User-Roles": "ROLE_ADMIN,ROLE_SCHOLARSHIP_OFFICER",
            "X-User-Email": "ingestion@prestiq.internal",
            "X-User-Id": "00000000-0000-0000-0000-000000000001",
        }

    async def submit_draft_for_review(
        self,
        normalized: NormalizedScholarshipData,
        source_url: str,
        content_hash: str,
        http_status: int = 200,
    ) -> int:
        """
        Submits candidate through the official 3-step administrative intake pipeline:
        1. POST /api/v1/admin/scholarships (create draft)
        2. POST /api/v1/admin/scholarships/{id}/sources (attach cryptographic provenance)
        3. PUT /api/v1/admin/scholarships/{id}/submit-review (place in PENDING_REVIEW queue)
        Returns the created scholarship ID.
        """
        headers = self._get_headers()
        client = self._client or httpx.AsyncClient(timeout=10.0)

        # 1. Prepare ScholarshipCreateRequest payload
        payload = {
            "title": normalized.title,
            "slug": normalized.slug,
            "providerName": normalized.provider_name,
            "description": normalized.description,
            "officialWebsiteUrl": normalized.official_website_url,
            "applicationUrl": normalized.application_url,
            "sourceType": normalized.source_type,
            "awardAmountMin": float(normalized.award_amount_min) if normalized.award_amount_min is not None else None,
            "awardAmountMax": float(normalized.award_amount_max) if normalized.award_amount_max is not None else None,
            "currencyCode": normalized.currency_code,
            "awardStructure": normalized.award_structure,
            "isRenewable": normalized.is_renewable,
            "applicationStartDate": normalized.application_start_date,
            "applicationDeadline": normalized.application_deadline,
            "academicYear": normalized.academic_year,
            "eligibleCountries": normalized.eligible_countries,
            "eligibleStates": normalized.eligible_states,
            "educationLevels": normalized.education_levels,
            "eligibleInstitutions": normalized.eligible_institutions,
            "eligibleFieldsOfStudy": normalized.eligible_fields_of_study,
            "minGpa": float(normalized.min_gpa) if normalized.min_gpa is not None else None,
            "gpaScale": float(normalized.gpa_scale),
            "maxFamilyIncome": float(normalized.max_family_income) if normalized.max_family_income is not None else None,
            "firstGenOnly": normalized.first_gen_only,
            "veteranOnly": normalized.veteran_only,
            "disabilityOnly": normalized.disability_only,
            "eligibilityCriteriaSchema": normalized.eligibility_criteria_schema,
            "sourceName": normalized.source_name,
        }

        try:
            # Step 1: Create Draft
            create_resp = await client.post(
                f"{self.base_url}/api/v1/admin/scholarships",
                json=payload,
                headers=headers,
            )

            if create_resp.status_code != 201:
                raise UpstreamFetchError(
                    f"scholarship-service rejected draft creation (HTTP {create_resp.status_code}): {create_resp.text}",
                    status_code=create_resp.status_code,
                )

            created_scholarship = create_resp.json()
            scholarship_id = created_scholarship["id"]

            # Step 2: Attach Provenance Source
            source_payload = {
                "sourceUrl": source_url,
                "sourceName": normalized.source_name or "Official Scrape Source",
                "contentHash": content_hash,
                "httpStatus": http_status,
            }
            source_resp = await client.post(
                f"{self.base_url}/api/v1/admin/scholarships/{scholarship_id}/sources",
                json=source_payload,
                headers=headers,
            )
            if source_resp.status_code != 201:
                logger.warning("Failed to attach source to scholarship %d (HTTP %d)", scholarship_id, source_resp.status_code)

            # Step 3: Advance to PENDING_REVIEW
            review_resp = await client.put(
                f"{self.base_url}/api/v1/admin/scholarships/{scholarship_id}/submit-review",
                headers=headers,
            )
            if review_resp.status_code != 200:
                logger.warning("Failed to advance scholarship %d to PENDING_REVIEW (HTTP %d)", scholarship_id, review_resp.status_code)

            logger.info("Successfully submitted candidate to scholarship-service: id=%d", scholarship_id)
            return scholarship_id

        except httpx.RequestError as e:
            raise UpstreamFetchError(f"Connection failure to scholarship-service at {self.base_url}: {str(e)}")
