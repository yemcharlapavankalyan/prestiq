"""Periodic ingestion runner with source freshness tracking and injectable clock."""

import asyncio
from datetime import datetime, timezone
import logging
from typing import Callable, List, Optional
from app.config import settings
from app.models.enums import RunTrigger
from app.pipeline.orchestrator import IngestionOrchestrator
from app.storage.database import get_session_factory
from app.storage.repository import IngestionRepository

logger = logging.getLogger("prestiq.ingestion.scheduler")


class IngestionScheduler:
    """Schedules background source polling and tracks source freshness."""

    def __init__(
        self,
        clock_fn: Optional[Callable[[], datetime]] = None,
        check_interval_seconds: int = 60,
    ):
        self.clock_fn = clock_fn or (lambda: datetime.now(timezone.utc))
        self.check_interval_seconds = check_interval_seconds
        self._running = False
        self._task: Optional[asyncio.Task] = None

    async def get_due_sources(self, repo: IngestionRepository) -> List[str]:
        """Identifies enabled sources due for scraping."""
        now = self.clock_fn()
        sources = await repo.list_sources(enabled_only=True)
        due = []
        for src in sources:
            if not src.last_attempted_at:
                due.append(src.id)
                continue

            last_attempt = src.last_attempted_at
            if last_attempt.tzinfo is None:
                last_attempt = last_attempt.replace(tzinfo=timezone.utc)

            elapsed_minutes = (now - last_attempt).total_seconds() / 60.0
            if elapsed_minutes >= src.refresh_interval_minutes:
                due.append(src.id)
        return due

    async def run_due_sources_once(self) -> int:
        """Executes a single check and runs all due sources up to max concurrency."""
        session_factory = get_session_factory()
        async with session_factory() as session:
            repo = IngestionRepository(session)
            due_ids = await self.get_due_sources(repo)

        if not due_ids:
            return 0

        logger.info("Found %d sources due for refresh: %s", len(due_ids), due_ids)
        orchestrator_tasks = []

        # Run bounded batch
        batch = due_ids[:settings.max_concurrent_runs]
        for src_id in batch:
            orchestrator_tasks.append(self._run_single_source(src_id))

        await asyncio.gather(*orchestrator_tasks, return_exceptions=True)
        return len(batch)

    async def _run_single_source(self, source_id: str) -> None:
        session_factory = get_session_factory()
        async with session_factory() as session:
            repo = IngestionRepository(session)
            orchestrator = IngestionOrchestrator(repo)
            try:
                await orchestrator.execute_run(source_id, trigger=RunTrigger.SCHEDULED)
                await session.commit()
            except Exception as e:
                logger.error("Scheduled run for %s failed: %s", source_id, str(e))
                await session.rollback()

    async def start(self) -> None:
        """Starts background loop."""
        self._running = True
        self._task = asyncio.create_task(self._loop())
        logger.info("Ingestion scheduler started.")

    async def stop(self) -> None:
        """Stops background loop."""
        self._running = False
        if self._task:
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass
        logger.info("Ingestion scheduler stopped.")

    async def _loop(self) -> None:
        while self._running:
            try:
                await self.run_due_sources_once()
            except Exception as e:
                logger.error("Error in scheduler loop: %s", str(e))
            await asyncio.sleep(self.check_interval_seconds)


scheduler = IngestionScheduler()
