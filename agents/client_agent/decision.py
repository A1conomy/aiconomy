"""Client agent decision logic — deterministic rules for tests."""

from decimal import Decimal


def should_accept_delivery(*, deliverable_notes: str, budget: Decimal) -> bool:
    """Accept when delivery notes are non-empty and mention deployment."""
    normalized = deliverable_notes.strip().lower()
    return bool(normalized) and "deploy" in normalized
