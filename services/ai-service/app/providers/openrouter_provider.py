"""OpenRouter API provider adapter for free-tier emergency fallback."""

import httpx
from typing import List
from app.config import settings
from app.core.exceptions import ProviderUnavailableError, RateLimitExceededError
from app.providers.base import LLMProvider, ProviderResponse


class OpenRouterProvider(LLMProvider):
    """OpenRouter adapter for free community models."""

    def __init__(
        self,
        api_key: str = settings.openrouter_api_key,
        model: str = settings.openrouter_model,
        rpm_limit: int = settings.openrouter_rpm_limit,
        base_url: str = settings.openrouter_base_url
    ):
        self.name = "openrouter"
        self.api_key = api_key
        self.model = model
        self.rpm_limit = rpm_limit
        self.base_url = base_url.rstrip("/")
        self.supports_embeddings = False

    async def generate(self, prompt: str, system_prompt: str = "") -> ProviderResponse:
        if not self.api_key:
            raise ProviderUnavailableError("OpenRouter API key is not configured")

        url = f"{self.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "HTTP-Referer": "https://prestiq.org",
            "X-Title": "PrestIQ AI Service",
            "Content-Type": "application/json"
        }

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": 0.1,
            "max_tokens": 800
        }

        async with httpx.AsyncClient(timeout=settings.http_timeout_seconds) as client:
            try:
                response = await client.post(url, headers=headers, json=payload)
                if response.status_code == 429:
                    raise RateLimitExceededError("OpenRouter free-tier rate limit exceeded")
                response.raise_for_status()

                data = response.json()
                choices = data.get("choices", [])
                if not choices:
                    raise ProviderUnavailableError("OpenRouter returned empty choices")

                content = choices[0].get("message", {}).get("content", "")
                usage = data.get("usage", {})
                return ProviderResponse(
                    content=content,
                    provider=self.name,
                    model=self.model,
                    input_tokens=usage.get("prompt_tokens", 0),
                    output_tokens=usage.get("completion_tokens", 0)
                )
            except httpx.HTTPStatusError as e:
                if e.response.status_code == 429:
                    raise RateLimitExceededError("OpenRouter rate limit exceeded")
                raise ProviderUnavailableError(f"OpenRouter API error: {e.response.text}")
            except httpx.RequestError as e:
                raise ProviderUnavailableError(f"OpenRouter connection failure: {str(e)}")

    async def embed(self, texts: List[str]) -> List[List[float]]:
        raise NotImplementedError("OpenRouter free tier does not guarantee embeddings")

    async def is_healthy(self) -> bool:
        return bool(self.api_key)
