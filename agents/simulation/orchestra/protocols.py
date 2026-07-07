"""Swappable agent interfaces for the orchestration demo."""

from __future__ import annotations

from typing import Iterator, Protocol

from simulation.orchestra.models import (
    ClientReviewResult,
    ManagerBid,
    TeamPlan,
    UserTaskRequest,
    ValidationResult,
)


class TaskIntakeAgent(Protocol):
    """Validates user tasks and estimates cost before marketplace entry."""

    def validate(self, request: UserTaskRequest) -> ValidationResult: ...


class ManagerPool(Protocol):
    """Collects bids from competing managers."""

    def collect_bids(
        self, request: UserTaskRequest, validation: ValidationResult
    ) -> tuple[ManagerBid, ...]: ...


class TeamAssembler(Protocol):
    """Winning manager builds team, plan, and price breakdown."""

    def build_plan(
        self,
        request: UserTaskRequest,
        validation: ValidationResult,
        winning_bid: ManagerBid,
    ) -> TeamPlan: ...


class ExecutionSimulator(Protocol):
    """Yields messages and step updates as the team executes work."""

    def run(
        self,
        request: UserTaskRequest,
        plan: TeamPlan,
    ) -> Iterator[tuple[str, object]]: ...


class ClientReviewAgent(Protocol):
    """Client accepts or rejects the final deliverable."""

    def review(
        self,
        request: UserTaskRequest,
        deliverables: tuple[str, ...],
        total_cost: str,
    ) -> ClientReviewResult: ...
