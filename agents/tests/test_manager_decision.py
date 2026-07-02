"""Tests for manager agent decision logic."""

from decimal import Decimal

from manager_agent.decision import propose_project_fee


def test_propose_project_fee_applies_markup() -> None:
    fee = propose_project_fee(budget=Decimal("400.00"))
    assert fee == Decimal("60.00")
