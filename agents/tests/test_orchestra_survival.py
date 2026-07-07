"""Tests for agent survival economics."""

from decimal import Decimal

from simulation.orchestra.mock.intake import MockTaskIntakeAgent
from simulation.orchestra.mock.managers import MockManagerPool
from simulation.orchestra.models import UserTaskRequest, ValidationResult
from simulation.orchestra.survival import (
    AgentEconomy,
    SimulationClock,
    daily_maintenance,
    minimum_acceptable_payout,
    minimum_task_budget,
)


def test_daily_maintenance_is_thirtieth_of_monthly() -> None:
    assert daily_maintenance() == Decimal("3.33")


def test_minimum_task_budget_scales_with_team_and_duration() -> None:
    floor = minimum_task_budget(duration_days=10, team_size=3)
    per_agent = minimum_acceptable_payout(10)
    assert floor == (per_agent * 3).quantize(Decimal("0.01"))


def test_clock_advances_one_day_per_second() -> None:
    clock = SimulationClock()
    assert clock.advance_seconds(2.0) == 2
    assert clock.elapsed_days == 2


def test_economy_charges_maintenance() -> None:
    economy = AgentEconomy()
    economy.register("a1", "Agent One", "WORKER", Decimal("250.00"))
    economy.apply_maintenance_all(5)
    wallet = economy.wallets["a1"]
    assert wallet.total_maintenance_paid == Decimal("16.65")
    assert wallet.balance == Decimal("233.35")


def test_intake_rejects_budget_below_survival_floor() -> None:
    agent = MockTaskIntakeAgent()
    result = agent.validate(
        UserTaskRequest(
            title="Tiny fix",
            description="Change one word on landing page.",
        )
    )
    assert result.accepted is False
    assert "survival" in result.reason.lower()


def test_managers_decline_when_fee_below_upkeep() -> None:
    pool = MockManagerPool()
    validation = ValidationResult(
        accepted=True,
        reason="ok",
        estimated_cost=Decimal("50.00"),
        required_skills=(),
        estimated_duration_days=30,
        survival_floor=Decimal("400.00"),
    )
    bids = pool.collect_bids(UserTaskRequest(title="Cheap", description="Small api backend task"), validation)
    assert all(not bid.accepted for bid in bids)
