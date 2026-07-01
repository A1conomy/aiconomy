"""Tests for ledger HTTP client."""

from decimal import Decimal
from uuid import UUID

import httpx

from common.clients.ledger import LedgerClient
from common.config import AgentConfig


def test_create_account_posts_to_ledger() -> None:
    config = AgentConfig(
        kafka_bootstrap_servers="localhost:9092",
        ledger_base_url="http://ledger.test",
        llm_provider="mock",
        ollama_base_url="http://localhost:11434",
        ollama_model="llama3",
        gemini_api_key="",
        gemini_model="gemini-2.0-flash",
    )

    def handler(request: httpx.Request) -> httpx.Response:
        assert request.url.path == "/api/v1/accounts"
        body = request.read().decode()
        assert "demo-firm" in body
        assert "FIRM" in body
        return httpx.Response(201, json={"id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"})

    transport = httpx.MockTransport(handler)
    client = httpx.Client(base_url=config.ledger_base_url, transport=transport)
    ledger = LedgerClient(config=config, client=client)

    account_id = ledger.create_account("demo-firm", "FIRM", Decimal("0.00"))

    assert account_id == UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
