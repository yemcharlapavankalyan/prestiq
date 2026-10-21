"""Mock LLM provider for unit and integration testing without external API dependencies."""

import hashlib
from typing import List, Optional
from app.providers.base import LLMProvider, ProviderResponse


class MockProvider(LLMProvider):
    """Deterministic mock provider for automated tests and offline execution."""

    def __init__(
        self,
        name: str = "mock",
        rpm_limit: int = 1000,
        supports_embeddings: bool = True,
        canned_response: Optional[str] = None
    ):
        self.name = name
        self.rpm_limit = rpm_limit
        self.supports_embeddings = supports_embeddings
        self.canned_response = canned_response
        self.simulated_error: Optional[Exception] = None
        self.call_count: int = 0
        self.last_prompt: Optional[str] = None
        self.last_system_prompt: Optional[str] = None

    async def generate(self, prompt: str, system_prompt: str = "") -> ProviderResponse:
        self.call_count += 1
        self.last_prompt = prompt
        self.last_system_prompt = system_prompt

        if self.simulated_error:
            raise self.simulated_error

        if self.canned_response is not None:
            content = self.canned_response
        else:
            # Deterministic default mock grounded answer
            content = (
                "According to the official guidelines, eligible students must maintain a minimum "
                "cumulative GPA of 3.5 [Source 1, Page 2]. The application deadline is December 1, 2026."
            )

        return ProviderResponse(
            content=content,
            provider=self.name,
            model="mock-v1",
            input_tokens=len(prompt.split()),
            output_tokens=len(content.split())
        )

    async def embed(self, texts: List[str]) -> List[List[float]]:
        if not self.supports_embeddings:
            raise NotImplementedError(f"{self.name} does not support embeddings")

        if self.simulated_error:
            raise self.simulated_error

        embeddings = []
        for text in texts:
            # Generate deterministic 768-dimensional normalized mock vector from text hash
            h = hashlib.sha256(text.encode("utf-8")).digest()
            # Expand hash bytes to 768 floats
            vec = []
            for i in range(768):
                byte_val = h[i % len(h)]
                vec.append((byte_val / 128.0) - 1.0)
            # Normalize vector
            norm = sum(x * x for x in vec) ** 0.5
            norm_vec = [x / norm for x in vec] if norm > 0 else vec
            embeddings.append(norm_vec)

        return embeddings

    async def is_healthy(self) -> bool:
        return self.simulated_error is None
