"""Groq Cloud API provider adapter for fast inference."""

import httpx
from typing import List
from app.config import settings
from app.core.exceptions import ProviderUnavailableError, RateLimitExceededError
from app.providers.base import LLMProvider, ProviderResponse


class GroqProvider(LLMProvider):
    """Groq Cloud adapter for Llama 3.3 70B and Llama 3.1 8B."""

    def __init__(
        self,
        api_key: str = settings.groq_api_key,
        model: str = settings.groq_model,
        fast_model: str = settings.groq_fast_model,
        rpm_limit: int = settings.groq_rpm_limit,
        base_url: str = settings.groq_base_url
    ):
        self.name = "groq"
        self.api_key = api_key
        self.model = model
        self.fast_model = fast_model
        self.rpm_limit = rpm_limit
        self.base_url = base_url.rstrip("/")
        self.supports_embeddings = False

    async def generate(self, prompt: str, system_prompt: str = "") -> ProviderResponse:
        if not self.api_key:
            raise ProviderUnavailableError("Groq API key is not configured")

        url = f"{self.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
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
                    raise RateLimitExceededError("Groq free-tier rate limit exceeded")
                response.raise_for_status()

                data = response.json()
                choices = data.get("choices", [])
                if not choices:
                    raise ProviderUnavailableError("Groq returned empty choices")

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
                    raise RateLimitExceededError("Groq rate limit exceeded")
                raise ProviderUnavailableError(f"Groq API error: {e.response.text}")
            except httpx.RequestError as e:
                raise ProviderUnavailableError(f"Groq connection failure: {str(e)}")

    async def embed(self, texts: List[str]) -> List[List[float]]:
        raise NotImplementedError("Groq does not provide embedding models")

    async def is_healthy(self) -> bool:
        return bool(self.api_key)
