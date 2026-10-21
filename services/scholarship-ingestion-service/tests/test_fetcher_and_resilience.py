"""Tests for polite HTTP fetcher, timeouts, rate limits, size guards, and backoff."""

import httpx
import pytest
from app.core.exceptions import (
    ResponseTooLargeError,
    SSRFSecurityError,
    UpstreamFetchError,
)
from app.pipeline.fetcher import PoliteFetcher


@pytest.mark.asyncio
async def test_successful_permitted_fetch(monkeypatch):
    # Mock transport returning valid HTML
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            status_code=200,
            headers={"Content-Type": "text/html; charset=utf-8"},
            content=b"<html><body><h1>Title</h1></body></html>",
        )

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    fetcher = PoliteFetcher(client=mock_client)

    result = await fetcher.fetch(
        url="https://studentaid.gov/scholarships",
        source_id="us-gov-studentaid",
        allowed_domains=["studentaid.gov"],
    )
    assert result.status_code == 200
    assert "Title" in result.content
    assert "text/html" in result.content_type


@pytest.mark.asyncio
async def test_oversized_response_aborts_with_payload_too_large():
    # Return oversized content (6 MB) when max is 1 MB
    large_payload = b"A" * (2 * 1024 * 1024)

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(
            status_code=200,
            headers={"Content-Type": "text/html"},
            content=large_payload,
        )

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    fetcher = PoliteFetcher(client=mock_client)

    with pytest.raises(ResponseTooLargeError, match="exceeds limit"):
        await fetcher.fetch(
            url="https://studentaid.gov/large.html",
            source_id="test",
            allowed_domains=["studentaid.gov"],
            max_bytes=1024 * 1024,  # 1 MB max
        )


@pytest.mark.asyncio
async def test_retry_on_429_with_retry_after_header():
    attempts = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal attempts
        attempts += 1
        if attempts == 1:
            return httpx.Response(
                status_code=429,
                headers={"Retry-After": "0.1"},
                content=b"Rate limited",
            )
        return httpx.Response(
            status_code=200,
            headers={"Content-Type": "text/html"},
            content=b"<html>Success after retry</html>",
        )

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    fetcher = PoliteFetcher(client=mock_client)

    result = await fetcher.fetch(
        url="https://studentaid.gov/portal",
        source_id="test",
        allowed_domains=["studentaid.gov"],
    )
    assert result.status_code == 200
    assert attempts == 2
    assert "Success after retry" in result.content


@pytest.mark.asyncio
async def test_permanent_404_not_retried_infinitely():
    attempts = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal attempts
        attempts += 1
        return httpx.Response(
            status_code=404,
            headers={"Content-Type": "text/html"},
            content=b"Not Found",
        )

    mock_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    fetcher = PoliteFetcher(client=mock_client)

    result = await fetcher.fetch(
        url="https://studentaid.gov/nonexistent",
        source_id="test",
        allowed_domains=["studentaid.gov"],
    )
    assert result.status_code == 404
    assert attempts == 1  # Should not be retried!
