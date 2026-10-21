"""Pydantic schemas for PrestIQ Scholarship Ingestion Service."""

from datetime import datetime
from decimal import Decimal
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field, HttpUrl
from app.models.enums import (
    CandidateDeduplicationStatus,
    CandidateSubmissionStatus,
    CandidateValidationStatus,
    ExtractionMethod,
    FetchStrategy,
    RunStatus,
    RunTrigger,
    SourceCategory,
)


class HealthResponse(BaseModel):
    """Health check response schema."""
    status: str = "UP"
    service: str = "scholarship-ingestion-service"
    version: str = "1.0.0"
    database: str = "UP"
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class SourceCreateRequest(BaseModel):
    """Request schema for registering an official scholarship source."""
    id: str = Field(..., min_length=3, max_length=100, pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$", description="Unique hyphenated slug")
    name: str = Field(..., min_length=3, max_length=255)
    base_url: str = Field(..., max_length=1000)
    category: SourceCategory
    enabled: bool = True
    fetch_strategy: FetchStrategy = FetchStrategy.STATIC_HTML
    allowed_domains: List[str] = Field(..., min_length=1, description="Strict domain whitelist for crawler & SSRF checks")
    refresh_interval_minutes: int = Field(1440, ge=10, le=43200)
    rate_limit_rpm: int = Field(15, ge=1, le=120)
    request_timeout_seconds: float = Field(15.0, ge=1.0, le=60.0)
    terms_of_use: Optional[str] = Field(None, max_length=2000)


class SourceUpdateRequest(BaseModel):
    """Request schema for updating an existing source registry entry."""
    name: Optional[str] = Field(None, min_length=3, max_length=255)
    base_url: Optional[str] = Field(None, max_length=1000)
    category: Optional[SourceCategory] = None
    enabled: Optional[bool] = None
    fetch_strategy: Optional[FetchStrategy] = None
    allowed_domains: Optional[List[str]] = None
    refresh_interval_minutes: Optional[int] = Field(None, ge=10, le=43200)
    rate_limit_rpm: Optional[int] = Field(None, ge=1, le=120)
    request_timeout_seconds: Optional[float] = Field(None, ge=1.0, le=60.0)
    terms_of_use: Optional[str] = None


class SourceResponse(BaseModel):
    """Source registry response representation."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    name: str
    base_url: str
    category: SourceCategory
    enabled: bool
    fetch_strategy: FetchStrategy
    allowed_domains: List[str]
    refresh_interval_minutes: int
    rate_limit_rpm: int
    request_timeout_seconds: float
    terms_of_use: Optional[str] = None
    last_attempted_at: Optional[datetime] = None
    last_successful_at: Optional[datetime] = None
    last_run_status: Optional[str] = None
    last_error_message: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class RunResponse(BaseModel):
    """Ingestion job run outcome representation."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    source_id: str
    trigger_type: RunTrigger
    status: RunStatus
    started_at: datetime
    completed_at: Optional[datetime] = None
    items_discovered: int = 0
    items_extracted: int = 0
    items_normalized: int = 0
    items_validated: int = 0
    items_duplicate: int = 0
    items_submitted: int = 0
    items_failed: int = 0
    error_message: Optional[str] = None


class RunListResponse(BaseModel):
    """Paginated or listed ingestion runs."""
    items: List[RunResponse]
    total: int


class ExtractedScholarshipData(BaseModel):
    """Raw structured fields extracted from document before conservative normalization."""
    title: Optional[str] = None
    provider_name: Optional[str] = None
    official_website_url: Optional[str] = None
    application_url: Optional[str] = None
    description: Optional[str] = None
    raw_deadline: Optional[str] = None
    raw_start_date: Optional[str] = None
    raw_amount: Optional[str] = None
    raw_currency: Optional[str] = None
    raw_education_levels: Optional[List[str]] = None
    raw_eligible_countries: Optional[List[str]] = None
    raw_eligible_states: Optional[List[str]] = None
    raw_min_gpa: Optional[str] = None
    raw_criteria: Optional[List[str]] = None
    evidence_snippets: Dict[str, str] = Field(default_factory=dict, description="Source text quotes proving extracted fields")


class NormalizedScholarshipData(BaseModel):
    """Normalized structured scholarship candidate matching scholarship-service schema."""
    title: str
    slug: str
    provider_name: str
    description: str
    official_website_url: str
    application_url: Optional[str] = None
    source_type: str = "GOVERNMENT"  # SourceCategory mapped
    award_amount_min: Optional[Decimal] = None
    award_amount_max: Optional[Decimal] = None
    currency_code: str = "USD"
    award_structure: str = "FIXED_AMOUNT"
    is_renewable: bool = False
    application_start_date: Optional[str] = None  # YYYY-MM-DD
    application_deadline: str  # YYYY-MM-DD
    academic_year: Optional[str] = None
    eligible_countries: List[str] = Field(default_factory=list)
    eligible_states: List[str] = Field(default_factory=list)
    education_levels: List[str] = Field(default_factory=list)
    eligible_institutions: List[str] = Field(default_factory=list)
    eligible_fields_of_study: List[str] = Field(default_factory=list)
    min_gpa: Optional[Decimal] = None
    gpa_scale: Decimal = Decimal("4.00")
    max_family_income: Optional[Decimal] = None
    first_gen_only: bool = False
    veteran_only: bool = False
    disability_only: bool = False
    eligibility_criteria_schema: List[Dict[str, Any]] = Field(default_factory=list)
    source_name: str = ""


class CandidateResponse(BaseModel):
    """Candidate scholarship model for review dashboard."""
    model_config = ConfigDict(from_attributes=True)

    id: str
    run_id: Optional[str] = None
    source_id: str
    source_url: str
    canonical_url: str
    content_hash: str
    title: str
    provider_name: str
    raw_data: Dict[str, Any]
    normalized_data: Dict[str, Any]
    extraction_method: ExtractionMethod
    extraction_model: Optional[str] = None
    confidence_score: float
    validation_status: CandidateValidationStatus
    validation_errors: List[str]
    deduplication_status: CandidateDeduplicationStatus
    scholarship_service_id: Optional[int] = None
    submission_status: CandidateSubmissionStatus
    submission_error: Optional[str] = None
    created_at: datetime
    updated_at: datetime


class CandidateListResponse(BaseModel):
    """Candidate list wrapper."""
    items: List[CandidateResponse]
    total: int


class ConflictResponse(BaseModel):
    """Candidate or source conflict detail."""
    candidate_id: str
    source_id: str
    canonical_url: str
    conflict_type: str  # e.g., "DEADLINE_CHANGED", "AMOUNT_CHANGED", "CRITERIA_MODIFIED"
    previous_value: Optional[str] = None
    new_value: Optional[str] = None
    detected_at: datetime
