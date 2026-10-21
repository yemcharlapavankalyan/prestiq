"""Tests for sliding window rate limiter."""

import pytest
from app.core.rate_limiter import SlidingWindowRateLimiter


@pytest.mark.asyncio
async def test_sliding_window_permits_under_limit():
    limiter = SlidingWindowRateLimiter()
    key = "test_key"

    assert await limiter.is_allowed(key, max_requests=3, window_seconds=10.0) is True

    await limiter.record_request(key, window_seconds=10.0)
    await limiter.record_request(key, window_seconds=10.0)

    assert await limiter.get_current_usage(key, window_seconds=10.0) == 2
    assert await limiter.is_allowed(key, max_requests=3, window_seconds=10.0) is True

    # 3rd request reaches limit
    await limiter.record_request(key, window_seconds=10.0)
    assert await limiter.is_allowed(key, max_requests=3, window_seconds=10.0) is False


@pytest.mark.asyncio
async def test_sliding_window_reset():
    limiter = SlidingWindowRateLimiter()
    key = "test_key"

    await limiter.record_request(key)
    assert await limiter.get_current_usage(key) == 1

    await limiter.reset(key)
    assert await limiter.get_current_usage(key) == 0
