"""Source Registry endpoints for Scholarship Ingestion Service."""

from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from app.api.deps import get_orchestrator, get_repository
from app.core.exceptions import SourceConflictError, SourceNotFoundError
from app.core.security import UserPrincipal, require_admin_or_officer
from app.core.ssrf_guard import validate_target_url
from app.models.db_models import SourceConfig
from app.models.enums import RunTrigger
from app.models.schemas import (
    RunResponse,
    SourceCreateRequest,
    SourceResponse,
    SourceUpdateRequest,
)
from app.pipeline.orchestrator import IngestionOrchestrator
from app.storage.repository import IngestionRepository

router = APIRouter(prefix="/api/v1/ingestion/sources", tags=["Source Registry"])


@router.get("", response_model=List[SourceResponse])
async def list_sources(
    enabled_only: bool = False,
    repo: IngestionRepository = Depends(get_repository),
):
    """Lists all configured scholarship sources."""
    sources = await repo.list_sources(enabled_only=enabled_only)
    return [SourceResponse.model_validate(s) for s in sources]


@router.get("/{source_id}", response_model=SourceResponse)
async def get_source(
    source_id: str,
    repo: IngestionRepository = Depends(get_repository),
):
    """Retrieves a specific source configuration."""
    source = await repo.get_source(source_id)
    if not source:
        raise SourceNotFoundError(source_id)
    return SourceResponse.model_validate(source)


@router.post("", response_model=SourceResponse, status_code=status.HTTP_201_CREATED)
async def create_source(
    request: SourceCreateRequest,
    repo: IngestionRepository = Depends(get_repository),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Registers an authorized official scholarship source."""
    existing = await repo.get_source(request.id)
    if existing:
        raise SourceConflictError(request.id)

    # Validate base URL against requested allowed domains (without DNS resolution on creation)
    validate_target_url(request.base_url, allowed_domains=request.allowed_domains, resolve_dns=False)

    source = SourceConfig(
        id=request.id,
        name=request.name,
        base_url=request.base_url,
        category=request.category.value,
        enabled=request.enabled,
        fetch_strategy=request.fetch_strategy.value,
        refresh_interval_minutes=request.refresh_interval_minutes,
        rate_limit_rpm=request.rate_limit_rpm,
        request_timeout_seconds=request.request_timeout_seconds,
        terms_of_use=request.terms_of_use,
    )
    source.allowed_domains = request.allowed_domains

    created = await repo.create_source(source)
    return SourceResponse.model_validate(created)


@router.patch("/{source_id}", response_model=SourceResponse)
async def update_source(
    source_id: str,
    request: SourceUpdateRequest,
    repo: IngestionRepository = Depends(get_repository),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Updates an existing source registry entry."""
    source = await repo.get_source(source_id)
    if not source:
        raise SourceNotFoundError(source_id)

    if request.name is not None:
        source.name = request.name
    if request.category is not None:
        source.category = request.category.value
    if request.enabled is not None:
        source.enabled = request.enabled
    if request.fetch_strategy is not None:
        source.fetch_strategy = request.fetch_strategy.value
    if request.allowed_domains is not None:
        source.allowed_domains = request.allowed_domains
    if request.refresh_interval_minutes is not None:
        source.refresh_interval_minutes = request.refresh_interval_minutes
    if request.rate_limit_rpm is not None:
        source.rate_limit_rpm = request.rate_limit_rpm
    if request.request_timeout_seconds is not None:
        source.request_timeout_seconds = request.request_timeout_seconds
    if request.terms_of_use is not None:
        source.terms_of_use = request.terms_of_use
    if request.base_url is not None:
        validate_target_url(request.base_url, allowed_domains=source.allowed_domains, resolve_dns=False)
        source.base_url = request.base_url

    updated = await repo.update_source(source)
    return SourceResponse.model_validate(updated)


@router.delete("/{source_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_source(
    source_id: str,
    repo: IngestionRepository = Depends(get_repository),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Removes a source configuration."""
    deleted = await repo.delete_source(source_id)
    if not deleted:
        raise SourceNotFoundError(source_id)


@router.post("/{source_id}/trigger", response_model=RunResponse)
async def trigger_source_run(
    source_id: str,
    orchestrator: IngestionOrchestrator = Depends(get_orchestrator),
    principal: UserPrincipal = Depends(require_admin_or_officer),
):
    """Manually triggers an immediate ingestion run for an authorized source."""
    run = await orchestrator.execute_run(source_id, trigger=RunTrigger.MANUAL)
    return RunResponse.model_validate(run)
