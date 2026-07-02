"""Kafka event DTOs matching Java records in aiconomy-common."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from decimal import Decimal
from enum import Enum
from typing import Any
from uuid import UUID, uuid4


class TaskSkill(str, Enum):
    BACKEND = "BACKEND"
    FRONTEND = "FRONTEND"
    DESIGN = "DESIGN"
    MANAGEMENT = "MANAGEMENT"
    COPYWRITING = "COPYWRITING"


@dataclass(frozen=True)
class TaskPostedEvent:
    event_id: UUID
    task_id: UUID
    project_id: UUID
    title: str
    description: str
    required_skill: TaskSkill
    budget: Decimal
    client_agent_id: str
    client_account_id: UUID
    posted_at: datetime

    @classmethod
    def create(
        cls,
        *,
        task_id: UUID,
        project_id: UUID,
        title: str,
        description: str,
        required_skill: TaskSkill,
        budget: Decimal,
        client_agent_id: str,
        client_account_id: UUID,
    ) -> TaskPostedEvent:
        return cls(
            event_id=uuid4(),
            task_id=task_id,
            project_id=project_id,
            title=title,
            description=description,
            required_skill=required_skill,
            budget=budget,
            client_agent_id=client_agent_id,
            client_account_id=client_account_id,
            posted_at=datetime.now(UTC),
        )

    def to_json_dict(self) -> dict[str, Any]:
        payload = asdict(self)
        return {
            "eventId": str(payload["event_id"]),
            "taskId": str(payload["task_id"]),
            "projectId": str(payload["project_id"]),
            "title": payload["title"],
            "description": payload["description"],
            "requiredSkill": payload["required_skill"].value,
            "budget": float(payload["budget"]),
            "clientAgentId": payload["client_agent_id"],
            "clientAccountId": str(payload["client_account_id"]),
            "postedAt": payload["posted_at"].isoformat() + "Z",
        }


@dataclass(frozen=True)
class PaymentProposedEvent:
    event_id: UUID
    task_id: UUID
    from_agent_id: str
    to_agent_id: str
    amount: Decimal
    rationale: str
    proposed_at: datetime

    def to_json_dict(self) -> dict[str, Any]:
        return {
            "eventId": str(self.event_id),
            "taskId": str(self.task_id),
            "fromAgentId": self.from_agent_id,
            "toAgentId": self.to_agent_id,
            "amount": float(self.amount),
            "rationale": self.rationale,
            "proposedAt": self.proposed_at.isoformat() + "Z",
        }


@dataclass(frozen=True)
class PaymentAcceptedEvent:
    event_id: UUID
    task_id: UUID
    from_agent_id: str
    to_agent_id: str
    amount: Decimal
    accepted_at: datetime

    def to_json_dict(self) -> dict[str, Any]:
        return {
            "eventId": str(self.event_id),
            "taskId": str(self.task_id),
            "fromAgentId": self.from_agent_id,
            "toAgentId": self.to_agent_id,
            "amount": float(self.amount),
            "acceptedAt": self.accepted_at.isoformat() + "Z",
        }
