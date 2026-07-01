"""Gemini-backed provider — demo / prod quality."""

import httpx

from common.config import AgentConfig, load_config
from common.llm.provider import LLMProvider, LLMResponse


class GeminiProvider(LLMProvider):
    def __init__(self, config: AgentConfig | None = None, client: httpx.Client | None = None) -> None:
        self._config = config or load_config()
        if not self._config.gemini_api_key:
            raise ValueError("GEMINI_API_KEY is required when LLM_PROVIDER=gemini")
        self._client = client or httpx.Client(timeout=60.0)
        self._owns_client = client is None

    def complete(self, prompt: str) -> LLMResponse:
        url = (
            "https://generativelanguage.googleapis.com/v1beta/models/"
            f"{self._config.gemini_model}:generateContent"
        )
        response = self._client.post(
            url,
            params={"key": self._config.gemini_api_key},
            json={"contents": [{"parts": [{"text": prompt}]}]},
        )
        response.raise_for_status()
        body = response.json()
        text = body["candidates"][0]["content"]["parts"][0]["text"]
        return LLMResponse(content=text)

    def close(self) -> None:
        if self._owns_client:
            self._client.close()
