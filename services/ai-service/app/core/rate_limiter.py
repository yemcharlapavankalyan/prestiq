"""In-memory sliding window rate limiter for provider RPM tracking and client throttling."""

import asyncio
import time
from collections import defaultdict
from typing import Dict, List


class SlidingWindowRateLimiter:
    """Thread-safe in-memory sliding window rate limiter."""

    def __init__(self):
        self._history: Dict[str, List[float]] = defaultdict(list)
        self._lock = asyncio.Lock()

    async def is_allowed(self, key: str, max_requests: int, window_seconds: float = 60.0) -> bool:
        """Check if request is permitted under rate limit window without recording."""
        async with self._lock:
            now = time.monotonic()
            cutoff = now - window_seconds
            # Purge expired timestamps
            self._history[key] = [t for t in self._history[key] if t > cutoff]
            return len(self._history[key]) < max_requests

    async def record_request(self, key: str, window_seconds: float = 60.0) -> None:
        """Record an execution timestamp for the given key."""
        async with self._lock:
            now = time.monotonic()
            cutoff = now - window_seconds
            self._history[key] = [t for t in self._history[key] if t > cutoff]
            self._history[key].append(now)

    async def get_current_usage(self, key: str, window_seconds: float = 60.0) -> int:
        """Get number of recorded calls within the active window."""
        async with self._lock:
            now = time.monotonic()
            cutoff = now - window_seconds
            self._history[key] = [t for t in self._history[key] if t > cutoff]
            return len(self._history[key])

    async def reset(self, key: str = None) -> None:
        """Clear rate limit history."""
        async with self._lock:
            if key:
                self._history.pop(key, None)
            else:
                self._history.clear()


rate_limiter = SlidingWindowRateLimiter()
