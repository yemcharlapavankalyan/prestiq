"""Domain Enums for Scholarship Ingestion Service."""

from enum import Enum


class SourceCategory(str, Enum):
    """Accredited categorization of the scholarship origin."""
    GOVERNMENT = "GOVERNMENT"
    UNIVERSITY = "UNIVERSITY"
    PRIVATE_FOUNDATION = "PRIVATE_FOUNDATION"
    CORPORATE = "CORPORATE"


class FetchStrategy(str, Enum):
    """Supported acquisition mechanism."""
    STATIC_HTML = "STATIC_HTML"
    RSS_FEED = "RSS_FEED"
    API_JSON = "API_JSON"


class RunTrigger(str, Enum):
    """Trigger mechanism for an ingestion job."""
    MANUAL = "MANUAL"
    SCHEDULED = "SCHEDULED"


class RunStatus(str, Enum):
    """Lifecycle state of an ingestion job."""
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELLED = "CANCELLED"


class CandidateValidationStatus(str, Enum):
    """Status of extracted candidate syntactic and semantic checks."""
    VALID = "VALID"
    INVALID = "INVALID"
    CONFLICT = "CONFLICT"


class CandidateDeduplicationStatus(str, Enum):
    """Result of duplicate check against known snapshots and candidates."""
    NEW = "NEW"
    EXACT_DUPLICATE = "EXACT_DUPLICATE"
    CONTENT_CHANGED = "CONTENT_CHANGED"


class CandidateSubmissionStatus(str, Enum):
    """Status of submission to scholarship-service review queue."""
    PENDING = "PENDING"
    SUBMITTED = "SUBMITTED"
    FAILED = "FAILED"
    NEEDS_REVIEW = "NEEDS_REVIEW"


class ExtractionMethod(str, Enum):
    """Method used to extract structured facts from scraped documents."""
    RULE_BASED = "RULE_BASED"
    LLM_ASSISTED = "LLM_ASSISTED"
    HYBRID = "HYBRID"
