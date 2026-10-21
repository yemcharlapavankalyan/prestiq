"""Thread-safe, sliding-window rate limiter per source."""

import asyncio
from collections import deque
import time
from typing import Dict


class SourceRateLimiter:
    """Sliding-window request rate limiter per source to prevent upstream flooding."""

    def __init__(self):
        self._history: Dict[str, deque] = {}
        self._lock = asyncio.Lock()

    async def acquire(self, source_id: str, limit_rpm: int) -> bool:
        """
        Records an attempt and checks if within limit_rpm.
        If exceeded, sleeps briefly to respect rate or returns False.
        """
        async with self._lock:
            now = time.monotonic()
            if source_id not in self._history:
                self._history[source_id] = deque()

            q = self._history[source_id]
            # Evict timestamps older than 60 seconds
            while q and now - q[0] > 60.0:
                q.popleft()

            if len(q) >= limit_rpm:
                return False

            q.append(now)
            return True

    async def wait_for_slot(self, source_id: str, limit_rpm: int, max_wait_seconds: float = 10.0) -> bool:
        """Waits asynchronously until a rate limit slot is available or timeout occurs."""
        start_time = time.monotonic()
        while time.monotonic() - start_time < max_wait_seconds:
            if await self.acquire(source_id, limit_rpm):
                return True
            await asyncio.sleep(0.5)
        return False


rate_limiter = SourceRateLimiter()
