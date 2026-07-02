"""Tests for worker agent decision logic."""

from decimal import Decimal

from common.events import TaskSkill
from worker_agent.decision import should_claim_task


def test_should_claim_when_skill_matches_and_budget_ok() -> None:
    assert should_claim_task(
        required_skill=TaskSkill.FRONTEND,
        worker_skill=TaskSkill.FRONTEND,
        budget=Decimal("400.00"),
    )


def test_should_not_claim_when_skill_mismatch() -> None:
    assert not should_claim_task(
        required_skill=TaskSkill.BACKEND,
        worker_skill=TaskSkill.FRONTEND,
        budget=Decimal("400.00"),
    )
