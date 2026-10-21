"""Tests for LLM provider abstraction, MockProvider, and multi-provider fallback."""

import pytest
from app.config import settings
from app.core.exceptions import ProviderUnavailableError, RateLimitExceededError
from app.core.rate_limiter import rate_limiter
from app.providers.factory import ProviderFactory
from app.providers.mock_provider import MockProvider


@pytest.mark.asyncio
async def test_mock_provider_generation_and_embedding():
    mock = MockProvider(name="mock", canned_response="Grounded test response [Source 1]")
    resp = await mock.generate("What is the GPA?", "System instructions")
    assert resp.content == "Grounded test response [Source 1]"
    assert resp.provider == "mock"
    assert mock.call_count == 1

    embeddings = await mock.embed(["Test text 1", "Test text 2"])
    assert len(embeddings) == 2
    assert len(embeddings[0]) == 768


@pytest.mark.asyncio
async def test_factory_fallback_on_rate_limit():
    factory = ProviderFactory()

    # Primary provider that hits rate limit
    primary = MockProvider(name="primary_mock", rpm_limit=1)
    # Fallback provider that succeeds
    fallback = MockProvider(name="fallback_mock", canned_response="Fallback response [Source 1]")

    factory.register_provider(primary)
    factory.register_provider(fallback)

    settings.primary_ai_provider = "primary_mock"
    settings.fallback_ai_provider = "fallback_mock"
    settings.use_mock_providers = False

    # First call uses primary
    resp1, fb1 = await factory.generate_with_fallback("Question 1")
    assert resp1.provider == "primary_mock"
    assert fb1 is False

    # Second call within same window hits primary RPM limit (1) -> routes to fallback
    resp2, fb2 = await factory.generate_with_fallback("Question 2")
    assert resp2.provider == "fallback_mock"
    assert fb2 is True


@pytest.mark.asyncio
async def test_factory_fallback_on_provider_error():
    factory = ProviderFactory()

    failing = MockProvider(name="failing_mock")
    failing.simulated_error = ProviderUnavailableError("Simulated provider outage")

    working = MockProvider(name="working_mock", canned_response="Working response")

    factory.register_provider(failing)
    factory.register_provider(working)

    settings.primary_ai_provider = "failing_mock"
    settings.fallback_ai_provider = "working_mock"
    settings.use_mock_providers = False

    resp, fb = await factory.generate_with_fallback("Question")
    assert resp.provider == "working_mock"
    assert fb is True


@pytest.mark.asyncio
async def test_all_providers_exhausted_raises_error():
    factory = ProviderFactory()

    p1 = MockProvider(name="p1")
    p1.simulated_error = RateLimitExceededError("Rate limit exhausted")
    p2 = MockProvider(name="p2")
    p2.simulated_error = RateLimitExceededError("Rate limit exhausted")

    factory.register_provider(p1)
    factory.register_provider(p2)

    settings.primary_ai_provider = "p1"
    settings.fallback_ai_provider = "p2"
    settings.tertiary_ai_provider = ""
    settings.emergency_ai_provider = ""
    settings.use_mock_providers = False

    with pytest.raises(RateLimitExceededError):
        await factory.generate_with_fallback("Question")
