"""Repository for Ingestion Service database access."""

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Tuple
from sqlalchemy import desc, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.db_models import (
    CandidateScholarship,
    IngestionRun,
    ScrapedSnapshot,
    SourceConfig,
)


class IngestionRepository:
    """Async repository encapsulating all data operations."""

    def __init__(self, session: AsyncSession):
        self.session = session

    # ==========================================
    # Sources
    # ==========================================

    async def get_source(self, source_id: str) -> Optional[SourceConfig]:
        stmt = select(SourceConfig).where(SourceConfig.id == source_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_sources(self, enabled_only: bool = False) -> List[SourceConfig]:
        stmt = select(SourceConfig)
        if enabled_only:
            stmt = stmt.where(SourceConfig.enabled.is_(True))
        stmt = stmt.order_by(SourceConfig.name)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def create_source(self, source: SourceConfig) -> SourceConfig:
        self.session.add(source)
        await self.session.flush()
        return source

    async def update_source(self, source: SourceConfig) -> SourceConfig:
        source.updated_at = datetime.now(timezone.utc)
        self.session.add(source)
        await self.session.flush()
        return source

    async def delete_source(self, source_id: str) -> bool:
        source = await self.get_source(source_id)
        if not source:
            return False
        await self.session.delete(source)
        await self.session.flush()
        return True

    # ==========================================
    # Runs
    # ==========================================

    async def create_run(self, run: IngestionRun) -> IngestionRun:
        self.session.add(run)
        await self.session.flush()
        return run

    async def get_run(self, run_id: str) -> Optional[IngestionRun]:
        stmt = select(IngestionRun).where(IngestionRun.id == run_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_runs(
        self,
        source_id: Optional[str] = None,
        status: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> Tuple[List[IngestionRun], int]:
        stmt = select(IngestionRun)
        count_stmt = select(func.count(IngestionRun.id))

        if source_id:
            stmt = stmt.where(IngestionRun.source_id == source_id)
            count_stmt = count_stmt.where(IngestionRun.source_id == source_id)
        if status:
            stmt = stmt.where(IngestionRun.status == status)
            count_stmt = count_stmt.where(IngestionRun.status == status)

        stmt = stmt.order_by(desc(IngestionRun.started_at)).limit(limit).offset(offset)

        total_res = await self.session.execute(count_stmt)
        total = total_res.scalar_one()

        items_res = await self.session.execute(stmt)
        items = list(items_res.scalars().all())
        return items, total

    async def update_run(self, run: IngestionRun) -> IngestionRun:
        self.session.add(run)
        await self.session.flush()
        return run

    # ==========================================
    # Snapshots
    # ==========================================

    async def create_snapshot(self, snapshot: ScrapedSnapshot) -> ScrapedSnapshot:
        self.session.add(snapshot)
        await self.session.flush()
        return snapshot

    async def get_latest_snapshot(self, source_url: str) -> Optional[ScrapedSnapshot]:
        stmt = (
            select(ScrapedSnapshot)
            .where(ScrapedSnapshot.source_url == source_url)
            .order_by(desc(ScrapedSnapshot.scraped_at))
            .limit(1)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    # ==========================================
    # Candidates
    # ==========================================

    async def create_candidate(self, candidate: CandidateScholarship) -> CandidateScholarship:
        self.session.add(candidate)
        await self.session.flush()
        return candidate

    async def get_candidate(self, candidate_id: str) -> Optional[CandidateScholarship]:
        stmt = select(CandidateScholarship).where(CandidateScholarship.id == candidate_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_candidate_by_canonical_url(self, canonical_url: str) -> Optional[CandidateScholarship]:
        stmt = (
            select(CandidateScholarship)
            .where(CandidateScholarship.canonical_url == canonical_url)
            .order_by(desc(CandidateScholarship.created_at))
            .limit(1)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def list_candidates(
        self,
        source_id: Optional[str] = None,
        run_id: Optional[str] = None,
        validation_status: Optional[str] = None,
        submission_status: Optional[str] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> Tuple[List[CandidateScholarship], int]:
        stmt = select(CandidateScholarship)
        count_stmt = select(func.count(CandidateScholarship.id))

        if source_id:
            stmt = stmt.where(CandidateScholarship.source_id == source_id)
            count_stmt = count_stmt.where(CandidateScholarship.source_id == source_id)
        if run_id:
            stmt = stmt.where(CandidateScholarship.run_id == run_id)
            count_stmt = count_stmt.where(CandidateScholarship.run_id == run_id)
        if validation_status:
            stmt = stmt.where(CandidateScholarship.validation_status == validation_status)
            count_stmt = count_stmt.where(CandidateScholarship.validation_status == validation_status)
        if submission_status:
            stmt = stmt.where(CandidateScholarship.submission_status == submission_status)
            count_stmt = count_stmt.where(CandidateScholarship.submission_status == submission_status)

        stmt = stmt.order_by(desc(CandidateScholarship.created_at)).limit(limit).offset(offset)

        total_res = await self.session.execute(count_stmt)
        total = total_res.scalar_one()

        items_res = await self.session.execute(stmt)
        items = list(items_res.scalars().all())
        return items, total

    async def update_candidate(self, candidate: CandidateScholarship) -> CandidateScholarship:
        candidate.updated_at = datetime.now(timezone.utc)
        self.session.add(candidate)
        await self.session.flush()
        return candidate

    async def list_conflicts(self, limit: int = 50, offset: int = 0) -> Tuple[List[CandidateScholarship], int]:
        stmt = (
            select(CandidateScholarship)
            .where(CandidateScholarship.deduplication_status == "CONTENT_CHANGED")
            .order_by(desc(CandidateScholarship.updated_at))
            .limit(limit)
            .offset(offset)
        )
        count_stmt = (
            select(func.count(CandidateScholarship.id))
            .where(CandidateScholarship.deduplication_status == "CONTENT_CHANGED")
        )
        total_res = await self.session.execute(count_stmt)
        total = total_res.scalar_one()
        items_res = await self.session.execute(stmt)
        return list(items_res.scalars().all()), total
