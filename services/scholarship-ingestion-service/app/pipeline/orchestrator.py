"""Pipeline Orchestrator managing end-to-end ingestion jobs."""

import asyncio
from datetime import datetime, timezone
import logging
from typing import Dict, Optional, Set
import uuid
from app.core.exceptions import (
    SourceExecutionLockedError,
    SourceNotFoundError,
    UpstreamFetchError,
)
from app.models.db_models import (
    CandidateScholarship,
    IngestionRun,
    ScrapedSnapshot,
)
from app.models.enums import (
    CandidateDeduplicationStatus,
    CandidateSubmissionStatus,
    CandidateValidationStatus,
    RunStatus,
    RunTrigger,
)
from app.pipeline.deduplicator import Deduplicator
from app.pipeline.fetcher import PoliteFetcher
from app.pipeline.llm_extractor import LlmAssistedExtractor
from app.pipeline.normalizer import ConservativeNormalizer
from app.pipeline.publisher import ScholarshipServicePublisher
from app.pipeline.validator import CandidateValidator
from app.storage.repository import IngestionRepository

logger = logging.getLogger("prestiq.ingestion.orchestrator")


class IngestionOrchestrator:
    """Orchestrates end-to-end source ingestion execution with concurrency locking."""

    _active_source_runs: Set[str] = set()
    _lock = asyncio.Lock()

    def __init__(
        self,
        repository: IngestionRepository,
        fetcher: Optional[PoliteFetcher] = None,
        extractor: Optional[LlmAssistedExtractor] = None,
        normalizer: Optional[ConservativeNormalizer] = None,
        validator: Optional[CandidateValidator] = None,
        deduplicator: Optional[Deduplicator] = None,
        publisher: Optional[ScholarshipServicePublisher] = None,
    ):
        self.repo = repository
        self.fetcher = fetcher or PoliteFetcher()
        self.extractor = extractor or LlmAssistedExtractor()
        self.normalizer = normalizer or ConservativeNormalizer()
        self.validator = validator or CandidateValidator()
        self.deduplicator = deduplicator or Deduplicator()
        self.publisher = publisher or ScholarshipServicePublisher()

    async def execute_run(
        self,
        source_id: str,
        trigger: RunTrigger = RunTrigger.MANUAL,
    ) -> IngestionRun:
        """
        Executes a complete ingestion run for a source:
        1. Lock check to prevent overlapping runs
        2. Create IngestionRun record
        3. Fetch document safely (SSRF + Rate limit + streaming size check)
        4. Save ScrapedSnapshot
        5. Extract structured fields
        6. Normalize conservatively
        7. Validate quality & check contradictory fields
        8. Deduplicate & change detection
        9. Submit valid opportunities to scholarship-service review queue
        10. Update operational metrics & release lock
        """
        # 1. Concurrency Lock Check
        async with self._lock:
            if source_id in self._active_source_runs:
                raise SourceExecutionLockedError(source_id)
            self._active_source_runs.add(source_id)

        source = await self.repo.get_source(source_id)
        if not source:
            async with self._lock:
                self._active_source_runs.remove(source_id)
            raise SourceNotFoundError(source_id)

        run_id = str(uuid.uuid4())
        now = datetime.now(timezone.utc)
        run = IngestionRun(
            id=run_id,
            source_id=source_id,
            trigger_type=trigger.value,
            status=RunStatus.RUNNING.value,
            started_at=now,
        )
        await self.repo.create_run(run)

        # Update source last attempted timestamp
        source.last_attempted_at = now
        source.last_run_status = RunStatus.RUNNING.value
        await self.repo.update_source(source)

        try:
            # 2. Safe Fetch
            fetch_res = await self.fetcher.fetch(
                url=source.base_url,
                source_id=source.id,
                allowed_domains=source.allowed_domains,
                rate_limit_rpm=source.rate_limit_rpm,
            )

            # 3. Snapshot Archival
            content_hash = self.deduplicator.compute_content_hash(fetch_res.content)
            snapshot = ScrapedSnapshot(
                run_id=run.id,
                source_id=source.id,
                source_url=fetch_res.url,
                content_hash=content_hash,
                raw_content=fetch_res.content,
                content_type=fetch_res.content_type,
                http_status=fetch_res.status_code,
                scraped_at=datetime.now(timezone.utc),
            )
            await self.repo.create_snapshot(snapshot)

            run.items_discovered = 1

            # If fetch returned client error (404, 403, etc.)
            if fetch_res.status_code >= 400:
                run.status = RunStatus.FAILED.value
                run.error_message = f"Upstream source returned HTTP {fetch_res.status_code}"
                run.items_failed = 1
                source.last_run_status = RunStatus.FAILED.value
                source.last_error_message = run.error_message
                await self.repo.update_run(run)
                await self.repo.update_source(source)
                return run

            # 4. Extraction
            extracted, method, model = await self.extractor.extract(
                html_or_text=fetch_res.content,
                source_url=fetch_res.url,
                default_provider_name=source.name,
            )
            run.items_extracted = 1

            # 5. Normalization
            normalized = self.normalizer.normalize(
                extracted=extracted,
                source_category=source.category,
                source_name=source.name,
            )
            run.items_normalized = 1

            # 6. Quality Validation
            val_status, val_errors, confidence = self.validator.validate(extracted, normalized)
            if val_status == CandidateValidationStatus.VALID:
                run.items_validated = 1

            # 7. Deduplication & Change Detection
            canonical_url = self.deduplicator.canonicalize_url(fetch_res.url)
            existing_candidate = await self.repo.get_candidate_by_canonical_url(canonical_url)
            dedup_status, conflict_summary = self.deduplicator.check_duplicate(
                canonical_url=canonical_url,
                content_hash=content_hash,
                normalized=normalized,
                existing_candidate=existing_candidate,
            )

            if dedup_status == CandidateDeduplicationStatus.EXACT_DUPLICATE:
                run.items_duplicate = 1

            # 8. Create Candidate Record
            candidate_id = str(uuid.uuid4())
            candidate = CandidateScholarship(
                id=candidate_id,
                run_id=run.id,
                source_id=source.id,
                source_url=fetch_res.url,
                canonical_url=canonical_url,
                content_hash=content_hash,
                title=normalized.title,
                provider_name=normalized.provider_name,
                extraction_method=method.value,
                extraction_model=model,
                confidence_score=confidence,
                validation_status=val_status.value,
                deduplication_status=dedup_status.value,
                submission_status=CandidateSubmissionStatus.PENDING.value,
            )
            candidate.raw_data = extracted.model_dump()
            candidate.normalized_data = normalized.model_dump()
            candidate.validation_errors = val_errors

            # 9. Submission to Scholarship Service (HITL Review Queue)
            if val_status == CandidateValidationStatus.VALID and dedup_status == CandidateDeduplicationStatus.NEW:
                try:
                    schol_id = await self.publisher.submit_draft_for_review(
                        normalized=normalized,
                        source_url=fetch_res.url,
                        content_hash=content_hash,
                        http_status=fetch_res.status_code,
                    )
                    candidate.scholarship_service_id = schol_id
                    candidate.submission_status = CandidateSubmissionStatus.SUBMITTED.value
                    run.items_submitted = 1
                except Exception as pub_err:
                    logger.error("Failed to submit candidate to scholarship-service: %s", str(pub_err))
                    candidate.submission_status = CandidateSubmissionStatus.FAILED.value
                    candidate.submission_error = str(pub_err)
                    run.items_failed += 1
            else:
                # Hold in review queue
                candidate.submission_status = CandidateSubmissionStatus.NEEDS_REVIEW.value

            await self.repo.create_candidate(candidate)

            # Update Run and Source
            run.status = RunStatus.COMPLETED.value
            run.completed_at = datetime.now(timezone.utc)
            source.last_successful_at = datetime.now(timezone.utc)
            source.last_run_status = RunStatus.COMPLETED.value
            source.last_error_message = None

            await self.repo.update_run(run)
            await self.repo.update_source(source)
            return run

        except Exception as e:
            logger.error("Ingestion run %s failed: %s", run.id, str(e), exc_info=True)
            run.status = RunStatus.FAILED.value
            run.completed_at = datetime.now(timezone.utc)
            run.error_message = str(e)
            run.items_failed += 1

            source.last_run_status = RunStatus.FAILED.value
            source.last_error_message = str(e)

            await self.repo.update_run(run)
            await self.repo.update_source(source)
            return run

        finally:
            async with self._lock:
                if source_id in self._active_source_runs:
                    self._active_source_runs.remove(source_id)
