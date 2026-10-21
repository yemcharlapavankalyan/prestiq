"""Abstract base class and data structures for LLM provider adapters."""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import List, Optional


@dataclass
class ProviderResponse:
    """Standardized generation output from an LLM provider."""

    content: str
    provider: str
    model: str
    input_tokens: int = 0
    output_tokens: int = 0


class LLMProvider(ABC):
    """Abstract interface for LLM inference and embeddings."""

    name: str
    rpm_limit: int
    supports_embeddings: bool = False

    @abstractmethod
    async def generate(self, prompt: str, system_prompt: str = "") -> ProviderResponse:
        """Generate text completion for given prompt and system instructions."""
        pass

    @abstractmethod
    async def embed(self, texts: List[str]) -> List[List[float]]:
        """Generate vector embeddings for input texts."""
        pass

    @abstractmethod
    async def is_healthy(self) -> bool:
        """Check provider connectivity and credential status."""
        pass
