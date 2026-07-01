"""Tests for LLM provider factory."""

from common.config import AgentConfig
from common.llm.factory import create_provider
from common.llm.gemini_provider import GeminiProvider
from common.llm.mock_provider import MockLLMProvider
from common.llm.ollama_provider import OllamaProvider


def test_create_mock_provider_by_default() -> None:
    provider = create_provider(AgentConfig(
        kafka_bootstrap_servers="localhost:9092",
        ledger_base_url="http://localhost:8081",
        llm_provider="mock",
        ollama_base_url="http://localhost:11434",
        ollama_model="llama3",
        gemini_api_key="",
        gemini_model="gemini-2.0-flash",
    ))
    assert isinstance(provider, MockLLMProvider)
    assert provider.complete("hello").content == "BUY"


def test_create_ollama_provider() -> None:
    provider = create_provider(AgentConfig(
        kafka_bootstrap_servers="localhost:9092",
        ledger_base_url="http://localhost:8081",
        llm_provider="ollama",
        ollama_base_url="http://localhost:11434",
        ollama_model="llama3",
        gemini_api_key="",
        gemini_model="gemini-2.0-flash",
    ))
    assert isinstance(provider, OllamaProvider)


def test_create_gemini_provider() -> None:
    provider = create_provider(AgentConfig(
        kafka_bootstrap_servers="localhost:9092",
        ledger_base_url="http://localhost:8081",
        llm_provider="gemini",
        ollama_base_url="http://localhost:11434",
        ollama_model="llama3",
        gemini_api_key="test-key",
        gemini_model="gemini-2.0-flash",
    ))
    assert isinstance(provider, GeminiProvider)
