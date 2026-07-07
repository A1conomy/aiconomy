"""Tests for client agent Kafka consumer."""

from decimal import Decimal
from unittest.mock import MagicMock
from uuid import UUID

from client_agent.consumer import handle_task_delivered, run_tasks_delivered_loop
from common.clients.tasks import TasksClient
from common.events import TaskDeliveredEvent


def _delivered_event() -> TaskDeliveredEvent:
    return TaskDeliveredEvent(
        event_id=UUID("11111111-1111-1111-1111-111111111111"),
        task_id=UUID("22222222-2222-2222-2222-222222222222"),
        agent_id="worker-1",
        deliverable_notes="Deployed landing page to staging",
        delivered_at=__import__("datetime").datetime.now(__import__("datetime").UTC),
    )


def test_handle_task_delivered_accepts_for_owning_client() -> None:
    tasks_client = MagicMock(spec=TasksClient)
    tasks_client.get_task.return_value = {
        "clientAgentId": "client-1",
        "budget": "400.00",
    }
    tasks_client.accept_task.return_value = {"status": "ACCEPTED"}

    result = handle_task_delivered(
        _delivered_event(),
        client_agent_id="client-1",
        tasks_client=tasks_client,
    )

    assert result is not None
    tasks_client.accept_task.assert_called_once()


def test_handle_task_delivered_skips_other_clients() -> None:
    tasks_client = MagicMock(spec=TasksClient)
    tasks_client.get_task.return_value = {"clientAgentId": "other-client", "budget": "400.00"}

    result = handle_task_delivered(
        _delivered_event(),
        client_agent_id="client-1",
        tasks_client=tasks_client,
    )

    assert result is None
    tasks_client.accept_task.assert_not_called()


def test_run_tasks_delivered_loop_processes_messages() -> None:
    consumer = MagicMock()
    consumer.consume_while.side_effect = lambda handler, max_messages=None, until_successes=None, poll_timeout=1.0: (
        1
        if handler(
            "tasks.delivered",
            {
                "eventId": "11111111-1111-1111-1111-111111111111",
                "taskId": "22222222-2222-2222-2222-222222222222",
                "agentId": "worker-1",
                "deliverableNotes": "Deployed landing page to staging",
                "deliveredAt": "2026-07-01T12:00:00Z",
            },
        )
        else 0
    )

    tasks_client = MagicMock(spec=TasksClient)
    tasks_client.get_task.return_value = {"clientAgentId": "client-1", "budget": "400.00"}
    tasks_client.accept_task.return_value = {"status": "ACCEPTED"}

    reviews = run_tasks_delivered_loop(
        client_agent_id="client-1",
        max_messages=1,
        tasks_client=tasks_client,
        consumer=consumer,
    )

    assert reviews == 1
