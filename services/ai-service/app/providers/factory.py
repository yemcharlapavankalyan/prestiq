"""Pluggable ProviderFactory with rate-limit tracking and graceful fallback routing."""

import logging
from typing import Dict, List, Optional, Tuple
from app.config import settings
from app.core.exceptions import ProviderUnavailableError, RateLimitExceededError
from app.core.rate_limiter import rate_limiter
from app.providers.base import LLMProvider, ProviderResponse
from app.providers.cerebras_provider import CerebrasProvider
from app.providers.gemini_provider import GeminiProvider
from app.providers.groq_provider import GroqProvider
from app.providers.mock_provider import MockProvider
from app.providers.openrouter_provider import OpenRouterProvider

logger = logging.getLogger(__name__)


class ProviderFactory:
    """Manages multi-provider registration, rate limit monitoring, and fallback routing."""

    def __init__(self):
        self._providers: Dict[str, LLMProvider] = {}
        self._init_default_providers()

    def _init_default_providers(self) -> None:
        """Register default candidate providers according to settings."""
        self._providers["mock"] = MockProvider()
        self._providers["gemini"] = GeminiProvider()
        self._providers["groq"] = GroqProvider()
        self._providers["cerebras"] = CerebrasProvider()
        self._providers["openrouter"] = OpenRouterProvider()

    def register_provider(self, provider: LLMProvider) -> None:
        """Register or override a provider adapter."""
        self._providers[provider.name] = provider

    def get_provider(self, name: str) -> Optional[LLMProvider]:
        """Retrieve registered provider by name."""
        return self._providers.get(name)

    def get_fallback_chain(self) -> List[str]:
        """Get ordered list of provider names to try for generation."""
        if settings.use_mock_providers:
            return ["mock"]

        chain = [
            settings.primary_ai_provider,
            settings.fallback_ai_provider,
            settings.tertiary_ai_provider,
            settings.emergency_ai_provider,
        ]
        # Remove duplicates while preserving order
        seen = set()
        ordered = []
        for p in chain:
            if p and p in self._providers and p not in seen:
                seen.add(p)
                ordered.append(p)
        return ordered

    async def generate_with_fallback(
        self,
        prompt: str,
        system_prompt: str = ""
    ) -> Tuple[ProviderResponse, bool]:
        """
        Generate completion using multi-provider fallback strategy.
        Returns (ProviderResponse, fallback_occurred).
        Enforces ALLOW_PAID_FALLBACK=false.
        """
        chain = self.get_fallback_chain()
        primary_name = chain[0] if chain else None
        last_exception = None

        for idx, provider_name in enumerate(chain):
            provider = self._providers.get(provider_name)
            if not provider:
                continue

            # 1. Proactive rate limit check
            is_allowed = await rate_limiter.is_allowed(
                key=provider.name,
                max_requests=provider.rpm_limit,
                window_seconds=60.0
            )

            if not is_allowed:
                logger.warning(
                    "Provider %s is approaching RPM limit (%d). Bypassing to next fallback.",
                    provider.name, provider.rpm_limit
                )
                continue

            # 2. Attempt generation
            try:
                response = await provider.generate(prompt=prompt, system_prompt=system_prompt)
                await rate_limiter.record_request(provider.name, window_seconds=60.0)
                fallback_occurred = (provider_name != primary_name)
                return response, fallback_occurred
            except (RateLimitExceededError, ProviderUnavailableError) as e:
                logger.warning("Provider %s failed: %s. Trying next in chain.", provider.name, str(e))
                last_exception = e
                continue
            except Exception as e:
                logger.error("Unexpected error with provider %s: %s", provider.name, str(e))
                last_exception = e
                continue

        # If all free providers are saturated or failed
        if isinstance(last_exception, RateLimitExceededError):
            raise last_exception
        raise ProviderUnavailableError("All configured free-tier AI providers are currently exhausted or unavailable")

    async def embed(self, texts: List[str]) -> List[List[float]]:
        """Generate vector embeddings using first available embedding provider."""
        if settings.use_mock_providers:
            mock_p = self._providers.get("mock")
            if mock_p and mock_p.supports_embeddings:
                return await mock_p.embed(texts)

        # Gemini is the primary embedding provider
        gemini_p = self._providers.get("gemini")
        if gemini_p and gemini_p.supports_embeddings and await gemini_p.is_healthy():
            return await gemini_p.embed(texts)

        # Fallback to mock embedding in development if Gemini key is missing
        mock_p = self._providers.get("mock")
        if mock_p:
            logger.info("Using mock embedding generator (Gemini API key not configured)")
            return await mock_p.embed(texts)

        raise ProviderUnavailableError("No embedding provider available")


provider_factory = ProviderFactory()
