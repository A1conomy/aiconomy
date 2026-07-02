"""Environment-backed configuration for Python agents."""

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class AgentConfig:
    kafka_bootstrap_servers: str
    ledger_base_url: str
    tasks_base_url: str
    llm_provider: str
    ollama_base_url: str
    ollama_model: str
    gemini_api_key: str
    gemini_model: str


def load_config() -> AgentConfig:
    return AgentConfig(
        kafka_bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"),
        ledger_base_url=os.getenv("LEDGER_BASE_URL", "http://localhost:8081").rstrip("/"),
        tasks_base_url=os.getenv("TASKS_BASE_URL", "http://localhost:8082").rstrip("/"),
        llm_provider=os.getenv("LLM_PROVIDER", "mock").lower(),
        ollama_base_url=os.getenv("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/"),
        ollama_model=os.getenv("OLLAMA_MODEL", "llama3"),
        gemini_api_key=os.getenv("GEMINI_API_KEY", ""),
        gemini_model=os.getenv("GEMINI_MODEL", "gemini-2.0-flash"),
    )
