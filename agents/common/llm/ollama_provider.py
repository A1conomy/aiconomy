"""Ollama-backed provider — dev only."""

import httpx

from common.config import AgentConfig, load_config
from common.llm.provider import LLMProvider, LLMResponse


class OllamaProvider(LLMProvider):
    def __init__(self, config: AgentConfig | None = None, client: httpx.Client | None = None) -> None:
        self._config = config or load_config()
        self._client = client or httpx.Client(base_url=self._config.ollama_base_url, timeout=60.0)
        self._owns_client = client is None

    def complete(self, prompt: str) -> LLMResponse:
        response = self._client.post(
            "/api/generate",
            json={"model": self._config.ollama_model, "prompt": prompt, "stream": False},
        )
        response.raise_for_status()
        return LLMResponse(content=response.json()["response"])

    def close(self) -> None:
        if self._owns_client:
            self._client.close()
