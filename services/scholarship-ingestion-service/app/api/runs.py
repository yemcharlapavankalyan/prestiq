"""Ingestion Runs operational endpoints."""

from typing import Optional
from fastapi import APIRouter, Depends, Query
from app.api.deps import get_orchestrator, get_repository
from app.core.exceptions import RunNotFoundError
from app.core.security import UserPrincipal, require_admin_or_officer
from app.models.enums import RunStatus, RunTrigger
from app.models.schemas import RunListResponse, RunResponse
from app.pipeline.orchestrator import IngestionOrchestrator
from app.storage.repository import IngestionRepository

router = APIRouter(prefix="/api/v1/ingestion/runs", tags=["Ingestion Runs"])


@router.get("", response_model=RunListResponse)
async def list_runs(
    source_id: Optional[str] = None,
    status: Optional[RunStatus] = None,
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    repo: IngestionRepository = Depends(get_repository),
):
    """Lists operational ingestion runs with filtering and pagination."""
    items, total = await repo.list_runs(
        source_id=source_id,
        status=status.value if status else None,
        limit=limit,
        offset=offset,
    )
    return RunListResponse(
        items=[RunResponse.model_validate(r) for r in items],
        total=total,
    )


@router.get("/{run_id}", response_model=RunResponse)
async def get_run(
    run_id: str,
    repo: IngestionRepository = Depends(get_repository),
):
    """Retrieves operational details and statistics for a specific run."""
    run = await repo.get_run(run_id)
    if not run:
        raise RunNotFoundError(run_id)
    return RunResponse.model_validate(run)


@router.post("/{run_id}/retry", response_model=RunResponse)
async def retry_run(
    run_id: str,
    orchestrator: IngestionOrchestrator = Depends(get_orchestrator),
    repo: IngestionRepository = Depends(get_repository),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Retries a failed ingestion run."""
    run = await repo.get_run(run_id)
    if not run:
        raise RunNotFoundError(run_id)

    new_run = await orchestrator.execute_run(run.source_id, trigger=RunTrigger.MANUAL)
    return RunResponse.model_validate(new_run)
