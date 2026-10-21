"""Polite, Safe HTTP Fetcher with SSRF Protection, Streaming Size Limit, and Retry."""

import asyncio
from dataclasses import dataclass
import logging
import random
import time
from typing import List, Optional
import httpx
from app.config import settings
from app.core.exceptions import (
    ResponseTooLargeError,
    SSRFSecurityError,
    UpstreamFetchError,
)
from app.core.rate_limiter import rate_limiter
from app.core.ssrf_guard import validate_target_url

logger = logging.getLogger("prestiq.ingestion.fetcher")


@dataclass
class FetchResult:
    """Outcome of fetching an external resource."""
    url: str
    status_code: int
    content: str
    content_type: str
    elapsed_seconds: float


class PoliteFetcher:
    """Safe, rate-limited HTTP fetcher enforcing SSRF defense and bounded resources."""

    def __init__(self, client: Optional[httpx.AsyncClient] = None):
        self._client = client

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is not None:
            return self._client
        return httpx.AsyncClient(
            headers={"User-Agent": settings.user_agent},
            timeout=httpx.Timeout(
                connect=settings.connect_timeout_seconds,
                read=settings.read_timeout_seconds,
                write=settings.connect_timeout_seconds,
                pool=settings.connect_timeout_seconds,
            ),
            follow_redirects=False,  # We manually validate each redirect to prevent SSRF redirect bypass
        )

    async def fetch(
        self,
        url: str,
        source_id: str,
        allowed_domains: List[str],
        rate_limit_rpm: int = 15,
        max_bytes: int = 5 * 1024 * 1024,
    ) -> FetchResult:
        """
        Fetches an external URL safely:
        1. SSRF check on initial target URL.
        2. Acquires rate limit slot.
        3. Streams response up to max_bytes.
        4. Handles redirects manually with SSRF checks on each hop.
        5. Retries retryable errors (429, 502, 503, 504, Timeout) with exponential backoff.
        """
        # Validate initial URL (skip DNS resolution if mock client is injected for offline testing)
        resolve_dns = (self._client is None)
        validate_target_url(url, allowed_domains=allowed_domains, resolve_dns=resolve_dns)

        # Wait for rate limit slot
        slot_ok = await rate_limiter.wait_for_slot(source_id, rate_limit_rpm)
        if not slot_ok:
            logger.warning("Rate limit wait timed out for source %s", source_id)

        client = await self._get_client()
        current_url = url
        redirect_hops = 0
        max_redirects = 5

        for attempt in range(1, settings.max_retries + 1):
            start_time = time.monotonic()
            try:
                # Streaming GET to guard against memory bombs
                async with client.stream("GET", current_url) as response:
                    elapsed = time.monotonic() - start_time

                    # Handle Redirects (301, 302, 303, 307, 308)
                    if response.status_code in (301, 302, 303, 307, 308):
                        redirect_hops += 1
                        if redirect_hops > max_redirects:
                            raise UpstreamFetchError(f"Exceeded max redirects ({max_redirects}) for URL: {url}")

                        redirect_target = response.headers.get("location")
                        if not redirect_target:
                            raise UpstreamFetchError("Redirect missing Location header.")

                        # Resolve relative redirect URLs if needed
                        resolved_redirect = str(response.url.join(redirect_target))
                        # SSRF check on the redirect destination!
                        validate_target_url(resolved_redirect, allowed_domains=allowed_domains)
                        current_url = resolved_redirect
                        continue

                    # Safe handling of 429
                    if response.status_code == 429:
                        retry_after = response.headers.get("Retry-After")
                        delay = float(retry_after) if retry_after and retry_after.isdigit() else (settings.retry_backoff_base ** attempt)
                        logger.warning("HTTP 429 Rate Limited from %s. Backing off for %.2f seconds.", current_url, delay)
                        if attempt < settings.max_retries:
                            await asyncio.sleep(delay)
                            continue
                        raise UpstreamFetchError(f"HTTP 429 Too Many Requests from {current_url}", status_code=429)

                    # Non-retryable permanent client errors
                    if response.status_code in (400, 401, 403, 404, 410):
                        content_bytes = await response.aread()
                        return FetchResult(
                            url=current_url,
                            status_code=response.status_code,
                            content=content_bytes.decode("utf-8", errors="replace"),
                            content_type=response.headers.get("content-type", "text/html"),
                            elapsed_seconds=elapsed,
                        )

                    # Retryable server errors (500, 502, 503, 504)
                    if response.status_code >= 500:
                        logger.warning("HTTP %d from %s (attempt %d/%d)", response.status_code, current_url, attempt, settings.max_retries)
                        if attempt < settings.max_retries:
                            delay = (settings.retry_backoff_base ** attempt) + random.uniform(0.1, 0.5)
                            await asyncio.sleep(delay)
                            continue
                        raise UpstreamFetchError(f"HTTP {response.status_code} server error from {current_url}", status_code=response.status_code)

                    # Content-Type Validation
                    content_type = response.headers.get("content-type", "").lower()
                    if not any(t in content_type for t in ("text/html", "application/xhtml+xml", "application/json", "text/plain")):
                        logger.warning("Unrecognized Content-Type: %s for %s", content_type, current_url)

                    # Bounded size read
                    chunks = []
                    total_bytes = 0
                    async for chunk in response.aiter_bytes():
                        total_bytes += len(chunk)
                        if total_bytes > max_bytes:
                            raise ResponseTooLargeError(total_bytes, max_bytes)
                        chunks.append(chunk)

                    raw_body = b"".join(chunks).decode("utf-8", errors="replace")
                    return FetchResult(
                        url=current_url,
                        status_code=response.status_code,
                        content=raw_body,
                        content_type=content_type,
                        elapsed_seconds=elapsed,
                    )

            except (httpx.ConnectTimeout, httpx.ReadTimeout, httpx.ConnectError) as net_err:
                logger.warning("Network error on %s: %s (attempt %d/%d)", current_url, str(net_err), attempt, settings.max_retries)
                if attempt < settings.max_retries:
                    delay = (settings.retry_backoff_base ** attempt) + random.uniform(0.1, 0.5)
                    await asyncio.sleep(delay)
                    continue
                raise UpstreamFetchError(f"Network timeout or connection error: {str(net_err)}", status_code=504)

        raise UpstreamFetchError(f"Failed to fetch {url} after {settings.max_retries} attempts.")
