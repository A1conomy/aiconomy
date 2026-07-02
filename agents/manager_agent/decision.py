"""Manager agent decision logic — price negotiation heuristics."""

from decimal import Decimal


def propose_project_fee(*, budget: Decimal, manager_markup_rate: Decimal = Decimal("0.15")) -> Decimal:
    """Propose a manager fee as a fraction of the client budget."""
    fee = (budget * manager_markup_rate).quantize(Decimal("0.01"))
    return max(fee, Decimal("1.00"))
