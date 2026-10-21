"""Pydantic schemas for request validation and response serialization."""

from typing import Optional
from pydantic import BaseModel, Field


class Citation(BaseModel):
    """Verified citation referencing an indexed document source."""

    sourceName: str = Field(..., description="Name of the official document or guideline")
    sourceUrl: str = Field(..., description="Canonical URL to the official source document")
    pageNumber: Optional[int] = Field(None, description="Page number where claim was verified")
    verifiedExcerpt: str = Field(..., description="Direct factual excerpt extracted from source")


class ChatRequest(BaseModel):
    """Inquiry regarding a specific verified scholarship opportunity."""

    scholarshipId: int = Field(..., description="Unique ID of the scholarship")
    question: str = Field(
        ...,
        min_length=1,
        max_length=400,
        description="Student question regarding eligibility, deadlines, requirements, or benefits"
    )


class ChatResponse(BaseModel):
    """Source-grounded synthesis with explicit citations and confidence."""

    answer: str = Field(..., description="Grounded answer synthesized from official guidelines")
    citations: list[Citation] = Field(default_factory=list, description="List of source citations supporting the claims")
    confidenceScore: float = Field(..., ge=0.0, le=1.0, description="Faithfulness / grounding confidence score")
    disclaimer: str = Field(
        default="Synthesized directly from verified provider documentation. Eligibility assessments are calculated deterministically by Prestiq's Eligibility Service.",
        description="Advisory disclaimer"
    )
    providerUsed: Optional[str] = Field(None, description="LLM provider that generated the response")
    fallbackOccurred: bool = Field(False, description="Whether fallback routing was triggered due to rate limits or cooldown")


class SourceDocument(BaseModel):
    """Raw source document metadata for indexing."""

    sourceId: int = Field(..., description="Identifier for this document")
    sourceUrl: str = Field(..., description="URL to the guidelines PDF or webpage")
    sourceName: Optional[str] = Field(None, description="Human readable name of the source")
    contentType: Optional[str] = Field("text/plain", description="MIME type of document content")
    content: Optional[str] = Field(None, description="Extracted raw text content")


class IndexScholarshipRequest(BaseModel):
    """Internal request to index verified scholarship sources."""

    scholarshipId: int = Field(..., description="Unique ID of the verified scholarship")
    sources: list[SourceDocument] = Field(..., description="List of verified guideline sources")


class IndexScholarshipResponse(BaseModel):
    """Status response after indexing scholarship documents."""

    scholarshipId: int
    chunksIndexed: int
    status: str = "INDEXED"


class HealthResponse(BaseModel):
    """Service health and capability status."""

    status: str
    service: str
    version: str
    activeProvider: str
    fallbackProvider: str
    database: str
