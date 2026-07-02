"""Worker agent decision logic — task selection heuristics."""

from decimal import Decimal

from common.events import TaskSkill


def should_claim_task(*, required_skill: TaskSkill, worker_skill: TaskSkill, budget: Decimal) -> bool:
    """Claim when skills match and budget meets minimum rate."""
    return required_skill == worker_skill and budget >= Decimal("50.00")
