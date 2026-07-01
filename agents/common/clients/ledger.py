"""HTTP client for the Core Banking Ledger (account bootstrap only)."""

from decimal import Decimal
from typing import Any
from uuid import UUID

import httpx

from common.config import AgentConfig, load_config


class LedgerClient:
    """Thin REST client for ledger account operations."""

    def __init__(self, config: AgentConfig | None = None, client: httpx.Client | None = None) -> None:
        self._config = config or load_config()
        self._client = client or httpx.Client(base_url=self._config.ledger_base_url, timeout=10.0)
        self._owns_client = client is None

    def create_account(self, owner_id: str, account_type: str, initial_balance: Decimal) -> UUID:
        response = self._client.post(
            "/api/v1/accounts",
            json={
                "ownerId": owner_id,
                "accountType": account_type,
                "initialBalance": float(initial_balance),
            },
        )
        response.raise_for_status()
        return UUID(response.json()["id"])

    def get_account(self, account_id: UUID) -> dict[str, Any]:
        response = self._client.get(f"/api/v1/accounts/{account_id}")
        response.raise_for_status()
        return response.json()

    def close(self) -> None:
        if self._owns_client:
            self._client.close()

    def __enter__(self) -> "LedgerClient":
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
