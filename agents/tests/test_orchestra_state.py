"""Tests for orchestration watch state snapshots."""

from decimal import Decimal

from common.events import TaskSkill
from simulation.orchestra.mock.managers import MockManagerPool
from simulation.orchestra.mock.registry import MANAGER_POOL
from simulation.orchestra.models import (
    OrchestrationPhase,
    UserTaskRequest,
    ValidationResult,
)
from simulation.orchestra.state import OrchestrationWatchState


def test_snapshot_includes_bids_and_graph() -> None:
    watch = OrchestrationWatchState(run_id="snap1")
    watch.set_request(UserTaskRequest(title="T", description="Landing page and API backend"))
    watch.set_validation(
        ValidationResult(
            accepted=True,
            reason="ok",
            estimated_cost=Decimal("500.00"),
            required_skills=(TaskSkill.FRONTEND, TaskSkill.BACKEND),
            estimated_duration_days=12,
            survival_floor=Decimal("137.97"),
        )
    )
    pool = MockManagerPool()
    bids = pool.collect_bids(watch.request, watch.validation)  # type: ignore[arg-type]
    for bid in bids:
        watch.add_bid(bid)
    winner = pool.select_winner(bids)
    watch.set_winning_bid(winner)
    watch.set_phase(OrchestrationPhase.MANAGER_BIDDING)

    snap = watch.snapshot()
    assert snap["phase"] == "manager_bidding"
    assert len(snap["bids"]) == len(MANAGER_POOL)
    assert any(bid["isWinner"] for bid in snap["bids"])  # type: ignore[index]
    assert len(snap["graph"]["nodes"]) >= 2  # type: ignore[index]
