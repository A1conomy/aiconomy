"""Thread-safe live state for the orchestration dashboard."""

from __future__ import annotations

import threading
from dataclasses import dataclass, field
from datetime import UTC, datetime
from decimal import Decimal

from simulation.orchestra.models import (
    AgentMessage,
    ExecutionStep,
    ManagerBid,
    MockBalances,
    OrchestrationPhase,
    TeamPlan,
    UserTaskRequest,
    ValidationResult,
)
from simulation.orchestra.survival import (
    MONTHLY_MAINTENANCE,
    AgentEconomy,
    SimulationClock,
    daily_maintenance,
)


@dataclass
class OrchestrationWatchState:
    """Rich snapshot consumed by the orchestra dashboard."""

    run_id: str
    phase: OrchestrationPhase = OrchestrationPhase.IDLE
    request: UserTaskRequest | None = None
    validation: ValidationResult | None = None
    bids: list[ManagerBid] = field(default_factory=list)
    winning_bid: ManagerBid | None = None
    team_plan: TeamPlan | None = None
    messages: list[AgentMessage] = field(default_factory=list)
    execution_steps: list[ExecutionStep] = field(default_factory=list)
    deliverables: list[str] = field(default_factory=list)
    client_feedback: str | None = None
    payment_released: bool | None = None
    balances: MockBalances | None = None
    events: list[str] = field(default_factory=list)
    error_message: str | None = None
    is_running: bool = False
    clock: SimulationClock | None = None
    economy: AgentEconomy | None = None
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def attach_survival(self, clock: SimulationClock, economy: AgentEconomy) -> None:
        with self._lock:
            self.clock = clock
            self.economy = economy

    def refresh_survival(self) -> None:
        return

    def set_phase(self, phase: OrchestrationPhase) -> None:
        with self._lock:
            self.phase = phase

    def set_request(self, request: UserTaskRequest) -> None:
        with self._lock:
            self.request = request

    def set_validation(self, validation: ValidationResult) -> None:
        with self._lock:
            self.validation = validation

    def add_bid(self, bid: ManagerBid) -> None:
        with self._lock:
            self.bids.append(bid)

    def set_winning_bid(self, bid: ManagerBid) -> None:
        with self._lock:
            self.winning_bid = bid

    def set_team_plan(self, plan: TeamPlan) -> None:
        with self._lock:
            self.team_plan = plan

    def add_message(self, message: AgentMessage) -> None:
        with self._lock:
            self.messages.append(message)

    def upsert_step(self, step: ExecutionStep) -> None:
        with self._lock:
            for index, existing in enumerate(self.execution_steps):
                if existing.agent_id == step.agent_id and existing.action == step.action:
                    self.execution_steps[index] = step
                    return
            self.execution_steps.append(step)

    def set_deliverables(self, items: tuple[str, ...]) -> None:
        with self._lock:
            self.deliverables = list(items)

    def set_client_outcome(self, *, accepted: bool, feedback: str) -> None:
        with self._lock:
            self.client_feedback = feedback
            self.payment_released = accepted

    def set_balances(self, balances: MockBalances) -> None:
        with self._lock:
            self.balances = balances

    def append_event(self, message: str) -> None:
        stamp = datetime.now(UTC).strftime("%H:%M:%S")
        line = f"[{stamp}] {message}"
        with self._lock:
            self.events.append(line)
            if len(self.events) > 40:
                self.events = self.events[-40:]

    def fail(self, message: str) -> None:
        with self._lock:
            self.phase = OrchestrationPhase.FAILED
            self.error_message = message
            self.is_running = False
            self.events.append(f"[FAIL] {message}")

    def reset_for_run(self, request: UserTaskRequest) -> None:
        with self._lock:
            self.phase = OrchestrationPhase.SUBMITTED
            self.request = request
            self.validation = None
            self.bids = []
            self.winning_bid = None
            self.team_plan = None
            self.messages = []
            self.execution_steps = []
            self.deliverables = []
            self.client_feedback = None
            self.payment_released = None
            self.balances = None
            self.error_message = None
            self.is_running = True
            self.clock = None
            self.economy = None

    def mark_idle(self) -> None:
        with self._lock:
            self.is_running = False

    def snapshot(self) -> dict[str, object]:
        with self._lock:
            return self._build_snapshot_locked()

    def _build_snapshot_locked(self) -> dict[str, object]:
        request = self.request
        validation = self.validation
        plan = self.team_plan
        balances = self.balances

        graph_nodes = self._graph_nodes_locked()
        graph_edges = self._graph_edges_locked()

        return {
            "runId": self.run_id,
            "phase": self.phase.value,
            "isRunning": self.is_running,
            "errorMessage": self.error_message,
            "request": None
            if request is None
            else {
                "title": request.title,
                "description": request.description,
                "clientId": request.client_id,
            },
            "validation": None
            if validation is None
            else {
                "accepted": validation.accepted,
                "reason": validation.reason,
                "estimatedCost": str(validation.estimated_cost) if validation.estimated_cost else None,
                "requiredSkills": [skill.value for skill in validation.required_skills],
                "estimatedDurationDays": validation.estimated_duration_days,
                "survivalFloor": str(validation.survival_floor) if validation.survival_floor else None,
            },
            "bids": [
                {
                    "managerId": bid.manager.manager_id,
                    "displayName": bid.manager.display_name,
                    "feePercent": str(bid.manager.fee_percent),
                    "feeAmount": str(bid.fee_amount),
                    "totalQuote": str(bid.total_quote),
                    "pitch": bid.pitch,
                    "isThinker": bid.manager.is_thinker,
                    "accepted": bid.accepted,
                    "declineReason": bid.decline_reason,
                    "isWinner": self.winning_bid is not None
                    and bid.manager.manager_id == self.winning_bid.manager.manager_id,
                }
                for bid in self.bids
            ],
            "teamPlan": None
            if plan is None
            else {
                "managerName": plan.manager.display_name,
                "managerFee": str(plan.manager_fee),
                "totalCost": str(plan.total_cost),
                "attackPlan": list(plan.attack_plan),
                "assignments": [
                    {
                        "agentId": assignment.agent.agent_id,
                        "displayName": assignment.agent.display_name,
                        "skill": assignment.agent.skill.value,
                        "subtask": assignment.subtask,
                        "payout": str(assignment.payout),
                    }
                    for assignment in plan.assignments
                ],
            },
            "messages": [
                {
                    "id": str(message.message_id),
                    "fromId": message.from_id,
                    "fromLabel": message.from_label,
                    "toId": message.to_id,
                    "toLabel": message.to_label,
                    "content": message.content,
                    "kind": message.kind,
                }
                for message in self.messages
            ],
            "executionSteps": [
                {
                    "id": str(step.step_id),
                    "agentId": step.agent_id,
                    "agentLabel": step.agent_label,
                    "action": step.action,
                    "status": step.status,
                    "deliverable": step.deliverable,
                }
                for step in self.execution_steps
            ],
            "deliverables": list(self.deliverables),
            "clientFeedback": self.client_feedback,
            "paymentReleased": self.payment_released,
            "balances": None
            if balances is None
            else {
                "clientBalance": str(balances.client_balance),
                "managerBalance": str(balances.manager_balance),
                "workerBalances": {agent_id: str(amount) for agent_id, amount in balances.worker_balances.items()},
            },
            "graph": {"nodes": graph_nodes, "edges": graph_edges},
            "survival": self._survival_snapshot_locked(),
            "events": list(self.events),
        }

    def _survival_snapshot_locked(self) -> dict[str, object]:
        clock = self.clock
        economy = self.economy
        return {
            "simDay": clock.elapsed_days if clock else 0,
            "dailyMaintenance": str(daily_maintenance()),
            "monthlyMaintenance": str(MONTHLY_MAINTENANCE),
            "rule": "1 second = 1 sim-day",
            "agents": economy.snapshot() if economy else [],
        }

    def _graph_nodes_locked(self) -> list[dict[str, str]]:
        nodes: dict[str, str] = {}
        if self.request is not None:
            nodes[self.request.client_id] = "Client"
        if self.winning_bid is not None:
            nodes[self.winning_bid.manager.manager_id] = self.winning_bid.manager.display_name
        for bid in self.bids:
            nodes[bid.manager.manager_id] = bid.manager.display_name
        if self.team_plan is not None:
            for assignment in self.team_plan.assignments:
                nodes[assignment.agent.agent_id] = assignment.agent.display_name
        return [{"id": node_id, "label": label} for node_id, label in nodes.items()]

    def _graph_edges_locked(self) -> list[dict[str, str]]:
        return [
            {
                "from": message.from_id,
                "to": message.to_id,
                "kind": message.kind,
                "label": message.kind,
            }
            for message in self.messages
        ]

    @staticmethod
    def initial_balances(client_balance: Decimal) -> MockBalances:
        return MockBalances(
            client_balance=client_balance,
            manager_balance=Decimal("0.00"),
            worker_balances={},
        )
