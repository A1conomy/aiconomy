"""Tests for mock manager bidding."""

from decimal import Decimal

from common.events import TaskSkill
from simulation.orchestra.mock.managers import MockManagerPool
from simulation.orchestra.models import UserTaskRequest, ValidationResult


def test_manager_pool_lowest_fee_wins() -> None:
    pool = MockManagerPool()
    request = UserTaskRequest(title="API", description="REST API with Postgres")
    validation = ValidationResult(
        accepted=True,
        reason="ok",
        estimated_cost=Decimal("600.00"),
        required_skills=(TaskSkill.BACKEND,),
        estimated_duration_days=10,
        survival_floor=Decimal("114.98"),
    )
    bids = pool.collect_bids(request, validation)
    winner = pool.select_winner(bids)
    assert len(bids) == 5
    assert winner.manager.fee_percent == Decimal("12.0")
    assert winner.manager.is_thinker is True
