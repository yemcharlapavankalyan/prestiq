"""Google Gemini API provider adapter for AI Studio free tier."""

import httpx
from typing import List
from app.config import settings
from app.core.exceptions import ProviderUnavailableError, RateLimitExceededError
from app.providers.base import LLMProvider, ProviderResponse


class GeminiProvider(LLMProvider):
    """Google Gemini AI Studio adapter (gemini-1.5-flash / text-embedding-004)."""

    def __init__(
        self,
        api_key: str = settings.gemini_api_key,
        model: str = settings.gemini_model,
        embedding_model: str = settings.gemini_embedding_model,
        rpm_limit: int = settings.gemini_rpm_limit,
        base_url: str = settings.gemini_base_url
    ):
        self.name = "gemini"
        self.api_key = api_key
        self.model = model
        self.embedding_model = embedding_model
        self.rpm_limit = rpm_limit
        self.base_url = base_url.rstrip("/")
        self.supports_embeddings = True

    async def generate(self, prompt: str, system_prompt: str = "") -> ProviderResponse:
        if not self.api_key:
            raise ProviderUnavailableError("Gemini API key is not configured")

        url = f"{self.base_url}/models/{self.model}:generateContent?key={self.api_key}"

        contents = []
        if system_prompt:
            contents.append({"role": "user", "parts": [{"text": f"System Instructions:\n{system_prompt}"}]})
            contents.append({"role": "model", "parts": [{"text": "Understood. I will strictly follow these instructions and ground my responses exclusively in the verified excerpts."}]})

        contents.append({"role": "user", "parts": [{"text": prompt}]})

        payload = {
            "contents": contents,
            "generationConfig": {
                "temperature": 0.1,
                "topP": 0.95,
                "maxOutputTokens": 800
            }
        }

        async with httpx.AsyncClient(timeout=settings.http_timeout_seconds) as client:
            try:
                response = await client.post(url, json=payload)
                if response.status_code == 429:
                    raise RateLimitExceededError("Gemini free-tier quota (15 RPM) exceeded")
                response.raise_for_status()

                data = response.json()
                candidates = data.get("candidates", [])
                if not candidates:
                    raise ProviderUnavailableError("Gemini returned empty candidate response")

                parts = candidates[0].get("content", {}).get("parts", [])
                text = "".join(part.get("text", "") for part in parts)

                usage = data.get("usageMetadata", {})
                return ProviderResponse(
                    content=text,
                    provider=self.name,
                    model=self.model,
                    input_tokens=usage.get("promptTokenCount", 0),
                    output_tokens=usage.get("candidatesTokenCount", 0)
                )
            except httpx.HTTPStatusError as e:
                if e.response.status_code == 429:
                    raise RateLimitExceededError("Gemini rate limit exceeded")
                raise ProviderUnavailableError(f"Gemini API error: {e.response.text}")
            except httpx.RequestError as e:
                raise ProviderUnavailableError(f"Gemini connection failure: {str(e)}")

    async def embed(self, texts: List[str]) -> List[List[float]]:
        if not self.api_key:
            raise ProviderUnavailableError("Gemini API key is not configured for embeddings")

        url = f"{self.base_url}/models/{self.embedding_model}:batchEmbedContents?key={self.api_key}"

        requests = [
            {"model": f"models/{self.embedding_model}", "content": {"parts": [{"text": t}]}}
            for t in texts
        ]

        async with httpx.AsyncClient(timeout=settings.http_timeout_seconds) as client:
            try:
                response = await client.post(url, json={"requests": requests})
                if response.status_code == 429:
                    raise RateLimitExceededError("Gemini embeddings quota exceeded")
                response.raise_for_status()

                data = response.json()
                embeddings_data = data.get("embeddings", [])
                return [item.get("values", []) for item in embeddings_data]
            except httpx.HTTPStatusError as e:
                if e.response.status_code == 429:
                    raise RateLimitExceededError("Gemini embeddings rate limit exceeded")
                raise ProviderUnavailableError(f"Gemini embedding error: {e.response.text}")
            except httpx.RequestError as e:
                raise ProviderUnavailableError(f"Gemini embedding connection failure: {str(e)}")

    async def is_healthy(self) -> bool:
        return bool(self.api_key)
