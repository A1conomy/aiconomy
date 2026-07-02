"""HTTP client for the task marketplace service."""

from __future__ import annotations

from decimal import Decimal
from typing import Any
from uuid import UUID

import httpx

from common.config import AgentConfig, load_config
from common.events import TaskSkill


class TasksClient:
    """Thin REST client for task board operations."""

    def __init__(self, config: AgentConfig | None = None, client: httpx.Client | None = None) -> None:
        self._config = config or load_config()
        self._client = client or httpx.Client(base_url=self._config.tasks_base_url, timeout=10.0)
        self._owns_client = client is None

    def post_task(
        self,
        *,
        project_id: UUID,
        title: str,
        description: str,
        required_skill: TaskSkill,
        budget: Decimal,
        client_agent_id: str,
        client_account_id: UUID,
    ) -> dict[str, Any]:
        response = self._client.post(
            "/api/v1/tasks",
            json={
                "projectId": str(project_id),
                "title": title,
                "description": description,
                "requiredSkill": required_skill.value,
                "budget": float(budget),
                "clientAgentId": client_agent_id,
                "clientAccountId": str(client_account_id),
            },
        )
        response.raise_for_status()
        return response.json()

    def list_open_tasks(self) -> list[dict[str, Any]]:
        response = self._client.get("/api/v1/tasks")
        response.raise_for_status()
        return response.json()

    def claim_task(self, task_id: UUID, *, agent_id: str, agent_account_id: UUID) -> dict[str, Any]:
        response = self._client.post(
            f"/api/v1/tasks/{task_id}/claim",
            json={"agentId": agent_id, "agentAccountId": str(agent_account_id)},
        )
        response.raise_for_status()
        return response.json()

    def deliver_task(self, task_id: UUID, *, agent_id: str, deliverable_notes: str) -> dict[str, Any]:
        response = self._client.post(
            f"/api/v1/tasks/{task_id}/deliver",
            json={"agentId": agent_id, "deliverableNotes": deliverable_notes},
        )
        response.raise_for_status()
        return response.json()

    def accept_task(self, task_id: UUID, *, client_agent_id: str) -> dict[str, Any]:
        response = self._client.post(
            f"/api/v1/tasks/{task_id}/accept",
            json={"clientAgentId": client_agent_id},
        )
        response.raise_for_status()
        return response.json()

    def reject_task(self, task_id: UUID, *, client_agent_id: str, reason: str) -> dict[str, Any]:
        response = self._client.post(
            f"/api/v1/tasks/{task_id}/reject",
            json={"clientAgentId": client_agent_id, "reason": reason},
        )
        response.raise_for_status()
        return response.json()

    def close(self) -> None:
        if self._owns_client:
            self._client.close()

    def __enter__(self) -> TasksClient:
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
