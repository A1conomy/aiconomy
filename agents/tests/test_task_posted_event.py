"""Tests for TaskPostedEvent JSON deserialization."""

from decimal import Decimal
from uuid import UUID

from common.events import TaskPostedEvent, TaskSkill


def test_from_json_dict_parses_java_payload() -> None:
    event = TaskPostedEvent.from_json_dict(
        {
            "eventId": "11111111-1111-1111-1111-111111111111",
            "taskId": "22222222-2222-2222-2222-222222222222",
            "projectId": "33333333-3333-3333-3333-333333333333",
            "title": "Landing page",
            "description": "Build page",
            "requiredSkill": "FRONTEND",
            "budget": 400.0,
            "clientAgentId": "client-1",
            "clientAccountId": "44444444-4444-4444-4444-444444444444",
            "postedAt": "2026-07-01T12:00:00Z",
        }
    )

    assert event.task_id == UUID("22222222-2222-2222-2222-222222222222")
    assert event.required_skill is TaskSkill.FRONTEND
    assert event.budget == Decimal("400.0")
