"""Tests for manager agent Kafka consumer."""

from unittest.mock import MagicMock, patch
from uuid import UUID

from common.events import TaskPostedEvent, TaskSkill
from manager_agent.consumer import handle_task_posted, run_tasks_posted_loop


def _posted_event(client_agent_id: str = "client-1") -> TaskPostedEvent:
    return TaskPostedEvent(
        event_id=UUID("11111111-1111-1111-1111-111111111111"),
        task_id=UUID("22222222-2222-2222-2222-222222222222"),
        project_id=UUID("33333333-3333-3333-3333-333333333333"),
        title="Landing page",
        description="Build page",
        required_skill=TaskSkill.FRONTEND,
        budget=__import__("decimal").Decimal("400.00"),
        client_agent_id=client_agent_id,
        client_account_id=UUID("44444444-4444-4444-4444-444444444444"),
        posted_at=__import__("datetime").datetime.now(__import__("datetime").UTC),
    )


@patch("manager_agent.consumer.negotiate_with_client")
def test_handle_task_posted_negotiates_for_matching_client(mock_negotiate: MagicMock) -> None:
    mock_negotiate.return_value = MagicMock(amount=__import__("decimal").Decimal("60.00"))
    producer = MagicMock()

    accepted = handle_task_posted(
        _posted_event(),
        manager_agent_id="manager-1",
        client_agent_id="client-1",
        producer=producer,
    )

    assert accepted is True
    mock_negotiate.assert_called_once()


@patch("manager_agent.consumer.negotiate_with_client")
def test_handle_task_posted_skips_other_client(mock_negotiate: MagicMock) -> None:
    producer = MagicMock()

    accepted = handle_task_posted(
        _posted_event(client_agent_id="other"),
        manager_agent_id="manager-1",
        client_agent_id="client-1",
        producer=producer,
    )

    assert accepted is False
    mock_negotiate.assert_not_called()


def test_run_tasks_posted_loop_processes_messages() -> None:
    consumer = MagicMock()
    consumer.consume_while.side_effect = lambda handler, max_messages=None, until_successes=None, poll_timeout=1.0: (
        1
        if handler(
            "tasks.posted",
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
            },
        )
        else 0
    )

    producer = MagicMock()

    with patch("manager_agent.consumer.negotiate_with_client") as mock_negotiate:
        mock_negotiate.return_value = MagicMock(amount=__import__("decimal").Decimal("60.00"))
        negotiations = run_tasks_posted_loop(
            manager_agent_id="manager-1",
            client_agent_id="client-1",
            max_messages=1,
            producer=producer,
            consumer=consumer,
        )

    assert negotiations == 1
