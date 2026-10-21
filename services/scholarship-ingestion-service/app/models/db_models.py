"""SQLAlchemy ORM models for PrestIQ Ingestion Service."""

from datetime import datetime, timezone
import json
from typing import Any, List, Optional
from sqlalchemy import (
    BigInteger,
    Boolean,
    Column,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
)
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    """Base class for all SQLAlchemy entities."""
    pass


def utc_now() -> datetime:
    """Returns current UTC timestamp."""
    return datetime.now(timezone.utc)


class SourceConfig(Base):
    """Registered, authorized scholarship data source."""
    __tablename__ = "source_configs"

    id = Column(String(100), primary_key=True)  # Stable slug, e.g. "us-gov-studentaid"
    name = Column(String(255), nullable=False)
    base_url = Column(String(1000), nullable=False)
    category = Column(String(50), nullable=False)  # SourceCategory
    enabled = Column(Boolean, nullable=False, default=True)
    fetch_strategy = Column(String(50), nullable=False, default="STATIC_HTML")
    allowed_domains_json = Column(Text, nullable=False, default="[]")
    refresh_interval_minutes = Column(Integer, nullable=False, default=1440)  # 24 hours
    rate_limit_rpm = Column(Integer, nullable=False, default=15)
    request_timeout_seconds = Column(Float, nullable=False, default=15.0)
    terms_of_use = Column(Text, nullable=True)

    # Operational metrics
    last_attempted_at = Column(DateTime(timezone=True), nullable=True)
    last_successful_at = Column(DateTime(timezone=True), nullable=True)
    last_run_status = Column(String(50), nullable=True)
    last_error_message = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=utc_now, onupdate=utc_now)

    runs = relationship("IngestionRun", back_populates="source", cascade="all, delete-orphan")

    @property
    def allowed_domains(self) -> List[str]:
        try:
            return json.loads(self.allowed_domains_json) if self.allowed_domains_json else []
        except Exception:
            return []

    @allowed_domains.setter
    def allowed_domains(self, domains: List[str]) -> None:
        self.allowed_domains_json = json.dumps(domains)


class IngestionRun(Base):
    """Execution instance of an ingestion job for a source."""
    __tablename__ = "ingestion_runs"

    id = Column(String(64), primary_key=True)  # UUID
    source_id = Column(String(100), ForeignKey("source_configs.id", ondelete="CASCADE"), nullable=False)
    trigger_type = Column(String(50), nullable=False, default="MANUAL")  # RunTrigger
    status = Column(String(50), nullable=False, default="PENDING")  # RunStatus

    started_at = Column(DateTime(timezone=True), nullable=False, default=utc_now)
    completed_at = Column(DateTime(timezone=True), nullable=True)

    # Metrics
    items_discovered = Column(Integer, nullable=False, default=0)
    items_extracted = Column(Integer, nullable=False, default=0)
    items_normalized = Column(Integer, nullable=False, default=0)
    items_validated = Column(Integer, nullable=False, default=0)
    items_duplicate = Column(Integer, nullable=False, default=0)
    items_submitted = Column(Integer, nullable=False, default=0)
    items_failed = Column(Integer, nullable=False, default=0)
    error_message = Column(Text, nullable=True)

    source = relationship("SourceConfig", back_populates="runs")
    snapshots = relationship("ScrapedSnapshot", back_populates="run", cascade="all, delete-orphan")
    candidates = relationship("CandidateScholarship", back_populates="run", cascade="all, delete-orphan")


class ScrapedSnapshot(Base):
    """Raw scraped content snapshot for audit, change detection, and legal provenance."""
    __tablename__ = "scraped_snapshots"

    id = Column(BigInteger().with_variant(Integer, "sqlite"), primary_key=True, autoincrement=True)
    run_id = Column(String(64), ForeignKey("ingestion_runs.id", ondelete="SET NULL"), nullable=True)
    source_id = Column(String(100), nullable=False)
    source_url = Column(String(1000), nullable=False, index=True)
    content_hash = Column(String(64), nullable=False, index=True)  # SHA-256
    raw_content = Column(Text, nullable=False)
    content_type = Column(String(100), nullable=False, default="text/html")
    http_status = Column(Integer, nullable=False, default=200)
    scraped_at = Column(DateTime(timezone=True), nullable=False, default=utc_now)

    run = relationship("IngestionRun", back_populates="snapshots")


class CandidateScholarship(Base):
    """Candidate scholarship record waiting for or submitted to administrative review."""
    __tablename__ = "candidate_scholarships"

    id = Column(String(64), primary_key=True)  # UUID
    run_id = Column(String(64), ForeignKey("ingestion_runs.id", ondelete="SET NULL"), nullable=True)
    source_id = Column(String(100), nullable=False)
    source_url = Column(String(1000), nullable=False)
    canonical_url = Column(String(1000), nullable=False, index=True)
    content_hash = Column(String(64), nullable=False)

    title = Column(String(255), nullable=False)
    provider_name = Column(String(255), nullable=False)

    raw_data_json = Column(Text, nullable=False, default="{}")
    normalized_data_json = Column(Text, nullable=False, default="{}")

    extraction_method = Column(String(50), nullable=False, default="RULE_BASED")
    extraction_model = Column(String(100), nullable=True)
    confidence_score = Column(Float, nullable=False, default=1.0)

    validation_status = Column(String(50), nullable=False, default="VALID")
    validation_errors_json = Column(Text, nullable=False, default="[]")

    deduplication_status = Column(String(50), nullable=False, default="NEW")
    scholarship_service_id = Column(BigInteger, nullable=True)  # Assigned by scholarship-service
    submission_status = Column(String(50), nullable=False, default="PENDING")
    submission_error = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), nullable=False, default=utc_now)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=utc_now, onupdate=utc_now)

    run = relationship("IngestionRun", back_populates="candidates")

    @property
    def raw_data(self) -> dict:
        try:
            return json.loads(self.raw_data_json) if self.raw_data_json else {}
        except Exception:
            return {}

    @raw_data.setter
    def raw_data(self, val: dict) -> None:
        self.raw_data_json = json.dumps(val, default=str)

    @property
    def normalized_data(self) -> dict:
        try:
            return json.loads(self.normalized_data_json) if self.normalized_data_json else {}
        except Exception:
            return {}

    @normalized_data.setter
    def normalized_data(self, val: dict) -> None:
        self.normalized_data_json = json.dumps(val, default=str)

    @property
    def validation_errors(self) -> List[str]:
        try:
            return json.loads(self.validation_errors_json) if self.validation_errors_json else []
        except Exception:
            return []

    @validation_errors.setter
    def validation_errors(self, errs: List[str]) -> None:
        self.validation_errors_json = json.dumps(errs, default=str)
