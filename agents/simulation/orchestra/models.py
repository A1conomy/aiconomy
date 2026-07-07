"""Domain models for the staged orchestration demo."""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal
from enum import Enum
from uuid import UUID, uuid4

from common.events import TaskSkill


class OrchestrationPhase(str, Enum):
    IDLE = "idle"
    SUBMITTED = "submitted"
    VALIDATING = "validating"
    REJECTED = "rejected"
    MANAGER_BIDDING = "manager_bidding"
    TEAM_PLANNING = "team_planning"
    EXECUTING = "executing"
    CLIENT_REVIEW = "client_review"
    PAID = "paid"
    PAYMENT_HELD = "payment_held"
    COMPLETE = "complete"
    FAILED = "failed"


@dataclass(frozen=True)
class UserTaskRequest:
    title: str
    description: str
    client_id: str = "demo-client"


@dataclass(frozen=True)
class ValidationResult:
    accepted: bool
    reason: str
    estimated_cost: Decimal | None = None
    required_skills: tuple[TaskSkill, ...] = ()
    estimated_duration_days: int = 0
    survival_floor: Decimal | None = None


@dataclass(frozen=True)
class SpecialistAgent:
    agent_id: str
    display_name: str
    skill: TaskSkill
    hourly_rate: Decimal
    bio: str


@dataclass(frozen=True)
class ManagerProfile:
    manager_id: str
    display_name: str
    fee_percent: Decimal
    tagline: str
    is_thinker: bool = False


@dataclass(frozen=True)
class ManagerBid:
    manager: ManagerProfile
    fee_amount: Decimal
    pitch: str
    total_quote: Decimal
    accepted: bool = True
    decline_reason: str | None = None


@dataclass(frozen=True)
class WorkAssignment:
    agent: SpecialistAgent
    subtask: str
    payout: Decimal


@dataclass(frozen=True)
class TeamPlan:
    manager: ManagerProfile
    manager_fee: Decimal
    assignments: tuple[WorkAssignment, ...]
    attack_plan: tuple[str, ...]
    total_cost: Decimal


@dataclass(frozen=True)
class AgentMessage:
    message_id: UUID
    from_id: str
    from_label: str
    to_id: str
    to_label: str
    content: str
    kind: str  # assign, deliver, verify, broadcast


@dataclass(frozen=True)
class ExecutionStep:
    step_id: UUID
    agent_id: str
    agent_label: str
    action: str
    status: str  # pending, active, done
    deliverable: str | None = None


@dataclass(frozen=True)
class ClientReviewResult:
    accepted: bool
    feedback: str


@dataclass(frozen=True)
class MockBalances:
    client_balance: Decimal
    manager_balance: Decimal
    worker_balances: dict[str, Decimal] = field(default_factory=dict)


@dataclass(frozen=True)
class OrchestrationResult:
    request: UserTaskRequest
    validation: ValidationResult
    winning_bid: ManagerBid | None = None
    team_plan: TeamPlan | None = None
    client_review: ClientReviewResult | None = None
    final_balances: MockBalances | None = None
    phase: OrchestrationPhase = OrchestrationPhase.COMPLETE


def new_message(
    *,
    from_id: str,
    from_label: str,
    to_id: str,
    to_label: str,
    content: str,
    kind: str,
) -> AgentMessage:
    return AgentMessage(
        message_id=uuid4(),
        from_id=from_id,
        from_label=from_label,
        to_id=to_id,
        to_label=to_label,
        content=content,
        kind=kind,
    )


def new_step(
    *,
    agent_id: str,
    agent_label: str,
    action: str,
    status: str = "pending",
    deliverable: str | None = None,
) -> ExecutionStep:
    return ExecutionStep(
        step_id=uuid4(),
        agent_id=agent_id,
        agent_label=agent_label,
        action=action,
        status=status,
        deliverable=deliverable,
    )
