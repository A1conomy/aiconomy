"""Tests for event JSON serialization."""

from decimal import Decimal
from uuid import UUID

from common.events import TaskPostedEvent, TaskSkill


def test_task_posted_event_json_uses_java_field_names() -> None:
    event = TaskPostedEvent.create(
        task_id=UUID("22222222-2222-2222-2222-222222222222"),
        project_id=UUID("33333333-3333-3333-3333-333333333333"),
        title="Landing page",
        description="Build page",
        required_skill=TaskSkill.FRONTEND,
        budget=Decimal("400.00"),
        client_agent_id="client-1",
        client_account_id=UUID("44444444-4444-4444-4444-444444444444"),
    )

    payload = event.to_json_dict()

    assert payload["requiredSkill"] == "FRONTEND"
    assert payload["clientAgentId"] == "client-1"
    assert payload["budget"] == 400.0
