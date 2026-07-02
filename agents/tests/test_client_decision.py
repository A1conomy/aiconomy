"""Tests for client agent decision logic."""

from decimal import Decimal

from client_agent.decision import should_accept_delivery


def test_should_accept_delivery_when_notes_mention_deploy() -> None:
    assert should_accept_delivery(
        deliverable_notes="Deployed landing page to staging",
        budget=Decimal("400.00"),
    )


def test_should_reject_delivery_when_notes_empty() -> None:
    assert not should_accept_delivery(deliverable_notes="   ", budget=Decimal("400.00"))
