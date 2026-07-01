"""Factory for LLM providers — switch via LLM_PROVIDER env var."""

from common.config import AgentConfig, load_config
from common.llm.gemini_provider import GeminiProvider
from common.llm.mock_provider import MockLLMProvider
from common.llm.ollama_provider import OllamaProvider
from common.llm.provider import LLMProvider


def create_provider(config: AgentConfig | None = None) -> LLMProvider:
    settings = config or load_config()
    if settings.llm_provider == "ollama":
        return OllamaProvider(config=settings)
    if settings.llm_provider == "gemini":
        return GeminiProvider(config=settings)
    return MockLLMProvider()
