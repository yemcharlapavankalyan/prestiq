"""Candidate scholarships review queue and conflict management endpoints."""

from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from app.api.deps import get_repository
from app.core.exceptions import CandidateNotFoundError
from app.core.security import UserPrincipal, require_admin_or_officer
from app.models.enums import (
    CandidateSubmissionStatus,
    CandidateValidationStatus,
)
from app.models.schemas import (
    CandidateListResponse,
    CandidateResponse,
    ConflictResponse,
    NormalizedScholarshipData,
)
from app.pipeline.publisher import ScholarshipServicePublisher
from app.storage.repository import IngestionRepository

router = APIRouter(prefix="/api/v1/ingestion", tags=["Candidates & Review Queue"])


@router.get("/candidates", response_model=CandidateListResponse)
async def list_candidates(
    source_id: Optional[str] = None,
    run_id: Optional[str] = None,
    validation_status: Optional[CandidateValidationStatus] = None,
    submission_status: Optional[CandidateSubmissionStatus] = None,
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    repo: IngestionRepository = Depends(get_repository),
):
    """Lists candidate scholarships for administrative review."""
    items, total = await repo.list_candidates(
        source_id=source_id,
        run_id=run_id,
        validation_status=validation_status.value if validation_status else None,
        submission_status=submission_status.value if submission_status else None,
        limit=limit,
        offset=offset,
    )
    return CandidateListResponse(
        items=[CandidateResponse.model_validate(c) for c in items],
        total=total,
    )


@router.get("/candidates/{candidate_id}", response_model=CandidateResponse)
async def get_candidate(
    candidate_id: str,
    repo: IngestionRepository = Depends(get_repository),
):
    """Retrieves detailed candidate data, evidence quotes, and validation errors."""
    candidate = await repo.get_candidate(candidate_id)
    if not candidate:
        raise CandidateNotFoundError(candidate_id)
    return CandidateResponse.model_validate(candidate)


@router.post("/candidates/{candidate_id}/submit", response_model=CandidateResponse)
async def submit_candidate(
    candidate_id: str,
    repo: IngestionRepository = Depends(get_repository),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Manually approves and submits a candidate to scholarship-service as a draft."""
    candidate = await repo.get_candidate(candidate_id)
    if not candidate:
        raise CandidateNotFoundError(candidate_id)

    if candidate.submission_status == CandidateSubmissionStatus.SUBMITTED.value:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Candidate {candidate_id} has already been submitted (Scholarship ID: {candidate.scholarship_service_id}).",
        )

    # Submit to scholarship-service review queue
    publisher = ScholarshipServicePublisher()
    normalized = NormalizedScholarshipData(**candidate.normalized_data)
    schol_id = await publisher.submit_draft_for_review(
        normalized=normalized,
        source_url=candidate.source_url,
        content_hash=candidate.content_hash,
    )

    candidate.scholarship_service_id = schol_id
    candidate.submission_status = CandidateSubmissionStatus.SUBMITTED.value
    candidate.submission_error = None
    updated = await repo.update_candidate(candidate)
    return CandidateResponse.model_validate(updated)


@router.get("/conflicts", response_model=List[ConflictResponse])
async def list_conflicts(
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    repo: IngestionRepository = Depends(get_repository),
):
    """Lists candidates where content changes or deadline shifts were detected against snapshots."""
    items, _ = await repo.list_conflicts(limit=limit, offset=offset)
    conflicts = []
    for c in items:
        conflicts.append(
            ConflictResponse(
                candidate_id=c.id,
                source_id=c.source_id,
                canonical_url=c.canonical_url,
                conflict_type="CONTENT_CHANGED",
                previous_value=None,
                new_value=c.normalized_data.get("application_deadline"),
                detected_at=c.updated_at,
            )
        )
    return conflicts
