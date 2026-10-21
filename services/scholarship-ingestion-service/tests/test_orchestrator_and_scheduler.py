"""Tests for pipeline orchestrator, run locking, scheduler, and operational errors."""

import asyncio
from datetime import datetime, timedelta, timezone
import httpx
import pytest
from app.core.exceptions import SourceExecutionLockedError
from app.models.db_models import SourceConfig
from app.models.enums import RunStatus, RunTrigger
from app.pipeline.fetcher import PoliteFetcher
from app.pipeline.orchestrator import IngestionOrchestrator
from app.pipeline.publisher import ScholarshipServicePublisher
from app.scheduler.runner import IngestionScheduler
from app.storage.repository import IngestionRepository


HTML_PAYLOAD = """
<!DOCTYPE html>
<html>
<head>
    <title>Autonomous Robotics Award 2027</title>
    <meta property="og:title" content="Autonomous Robotics Award 2027" />
    <meta property="og:site_name" content="Robotics Institute" />
    <meta name="description" content="Grant for graduate students conducting thesis research in robotics." />
</head>
<body>
    <h1>Autonomous Robotics Award 2027</h1>
    <p>Award Value: $8,000</p>
    <p>Application Deadline: November 15, 2027</p>
    <p>Minimum GPA of 3.60 required.</p>
</body>
</html>
"""


@pytest.mark.asyncio
async def test_orchestrator_executes_successful_end_to_end_run(
    test_repo: IngestionRepository,
    seeded_source: SourceConfig,
):
    # Mock external web page fetch
    def web_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, headers={"Content-Type": "text/html"}, content=HTML_PAYLOAD.encode("utf-8"))

    mock_fetch_client = httpx.AsyncClient(transport=httpx.MockTransport(web_handler))
    fetcher = PoliteFetcher(client=mock_fetch_client)

    # Mock scholarship service intake
    def schol_handler(request: httpx.Request) -> httpx.Response:
        if request.url.path == "/api/v1/admin/scholarships":
            return httpx.Response(201, json={"id": 888, "title": "Autonomous Robotics Award 2027"})
        elif "/sources" in request.url.path:
            return httpx.Response(201, json={"id": 1})
        elif "/submit-review" in request.url.path:
            return httpx.Response(200, json={"id": 888, "verificationStatus": "PENDING_REVIEW"})
        return httpx.Response(404)

    mock_schol_client = httpx.AsyncClient(transport=httpx.MockTransport(schol_handler))
    publisher = ScholarshipServicePublisher(client=mock_schol_client)

    orchestrator = IngestionOrchestrator(
        repository=test_repo,
        fetcher=fetcher,
        publisher=publisher,
    )

    run = await orchestrator.execute_run(seeded_source.id, trigger=RunTrigger.MANUAL)
    assert run.status == RunStatus.COMPLETED.value
    assert run.items_discovered == 1
    assert run.items_extracted == 1
    assert run.items_validated == 1
    assert run.items_submitted == 1
    assert run.error_message is None

    # Verify snapshot was archived
    snapshot = await test_repo.get_latest_snapshot(seeded_source.base_url)
    assert snapshot is not None
    assert "Autonomous Robotics" in snapshot.raw_content

    # Verify candidate record created with assigned scholarship_service_id
    candidate = await test_repo.get_candidate_by_canonical_url(seeded_source.base_url)
    assert candidate is not None
    assert candidate.scholarship_service_id == 888
    assert candidate.submission_status == "SUBMITTED"


@pytest.mark.asyncio
async def test_orchestrator_prevents_overlapping_runs_for_same_source(
    test_repo: IngestionRepository,
    seeded_source: SourceConfig,
):
    # Simulate a slow fetch to test concurrency lock
    async def slow_fetch(*args, **kwargs):
        await asyncio.sleep(0.5)
        raise ConnectionError("Aborted")

    fetcher = PoliteFetcher()
    fetcher.fetch = slow_fetch

    orchestrator = IngestionOrchestrator(repository=test_repo, fetcher=fetcher)

    # Launch first run in background
    task1 = asyncio.create_task(orchestrator.execute_run(seeded_source.id))
    await asyncio.sleep(0.05)

    # Second run for same source must fail immediately with SourceExecutionLockedError
    with pytest.raises(SourceExecutionLockedError, match="Overlapping runs are prohibited"):
        await orchestrator.execute_run(seeded_source.id)

    await task1


@pytest.mark.asyncio
async def test_orchestrator_records_sanitized_operational_error_on_fetch_failure(
    test_repo: IngestionRepository,
    seeded_source: SourceConfig,
):
    def failing_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(502, content=b"Bad Gateway from target portal")

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(failing_handler))
    fetcher = PoliteFetcher(client=mock_client)
    orchestrator = IngestionOrchestrator(repository=test_repo, fetcher=fetcher)

    run = await orchestrator.execute_run(seeded_source.id)
    assert run.status == RunStatus.FAILED.value
    assert "502" in (run.error_message or "")
    assert run.items_failed == 1


@pytest.mark.asyncio
async def test_scheduler_detects_due_sources_with_injectable_clock(
    test_repo: IngestionRepository,
    seeded_source: SourceConfig,
):
    # Source last attempted 2 days ago
    past_time = datetime.now(timezone.utc) - timedelta(days=2)
    seeded_source.last_attempted_at = past_time
    await test_repo.update_source(seeded_source)

    # Injected clock is current time
    fixed_now = datetime.now(timezone.utc)
    scheduler = IngestionScheduler(clock_fn=lambda: fixed_now)

    due = await scheduler.get_due_sources(test_repo)
    assert seeded_source.id in due

    # Advance source last attempted to 1 hour ago (refresh interval is 1440 min = 24h)
    seeded_source.last_attempted_at = fixed_now - timedelta(hours=1)
    await test_repo.update_source(seeded_source)

    not_due = await scheduler.get_due_sources(test_repo)
    assert seeded_source.id not in not_due
