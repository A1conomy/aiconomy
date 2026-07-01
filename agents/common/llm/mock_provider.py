"""Deterministic LLM for tests and local dev without Ollama."""

from common.llm.provider import LLMProvider, LLMResponse


class MockLLMProvider(LLMProvider):
    def __init__(self, response: str = "BUY") -> None:
        self._response = response

    def complete(self, prompt: str) -> LLMResponse:
        return LLMResponse(content=self._response)
