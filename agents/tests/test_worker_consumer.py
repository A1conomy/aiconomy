"""Tests for worker agent Kafka consumer logic."""

from decimal import Decimal
from unittest.mock import MagicMock
from uuid import UUID

import httpx

from common.clients.tasks import TasksClient
from common.events import TaskPostedEvent, TaskSkill
from worker_agent.consumer import handle_task_posted, run_tasks_posted_loop


def _sample_event(skill: TaskSkill = TaskSkill.FRONTEND, budget: Decimal = Decimal("400.00")) -> TaskPostedEvent:
    return TaskPostedEvent(
        event_id=UUID("11111111-1111-1111-1111-111111111111"),
        task_id=UUID("22222222-2222-2222-2222-222222222222"),
        project_id=UUID("33333333-3333-3333-3333-333333333333"),
        title="Landing page",
        description="Build page",
        required_skill=skill,
        budget=budget,
        client_agent_id="client-1",
        client_account_id=UUID("44444444-4444-4444-4444-444444444444"),
        posted_at=__import__("datetime").datetime.now(__import__("datetime").UTC),
    )


def test_handle_task_posted_claims_matching_task() -> None:
    tasks_client = MagicMock(spec=TasksClient)
    tasks_client.claim_task.return_value = {"id": "22222222-2222-2222-2222-222222222222", "status": "CLAIMED"}

    result = handle_task_posted(
        _sample_event(),
        worker_agent_id="worker-1",
        worker_account_id=UUID("55555555-5555-5555-5555-555555555555"),
        worker_skill=TaskSkill.FRONTEND,
        tasks_client=tasks_client,
    )

    assert result is not None
    tasks_client.claim_task.assert_called_once()


def test_handle_task_posted_skips_skill_mismatch() -> None:
    tasks_client = MagicMock(spec=TasksClient)

    result = handle_task_posted(
        _sample_event(skill=TaskSkill.BACKEND),
        worker_agent_id="worker-1",
        worker_account_id=UUID("55555555-5555-5555-5555-555555555555"),
        worker_skill=TaskSkill.FRONTEND,
        tasks_client=tasks_client,
    )

    assert result is None
    tasks_client.claim_task.assert_not_called()


def test_handle_task_posted_ignores_already_claimed_conflict() -> None:
    tasks_client = MagicMock(spec=TasksClient)
    response = httpx.Response(409, request=httpx.Request("POST", "http://tasks.test/claim"))
    tasks_client.claim_task.side_effect = httpx.HTTPStatusError(
        "conflict", request=response.request, response=response
    )

    result = handle_task_posted(
        _sample_event(),
        worker_agent_id="worker-1",
        worker_account_id=UUID("55555555-5555-5555-5555-555555555555"),
        worker_skill=TaskSkill.FRONTEND,
        tasks_client=tasks_client,
    )

    assert result is None


def test_run_tasks_posted_loop_processes_kafka_messages() -> None:
    consumer = MagicMock()
    consumer.consume_while.side_effect = lambda handler, max_messages=None, until_successes=None, poll_timeout=1.0: (
        1 if handler(
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

    tasks_client = MagicMock(spec=TasksClient)
    tasks_client.claim_task.return_value = {"status": "CLAIMED"}
    tasks_client.deliver_task.return_value = {"status": "DELIVERED"}

    claims = run_tasks_posted_loop(
        worker_agent_id="worker-1",
        worker_account_id=UUID("55555555-5555-5555-5555-555555555555"),
        worker_skill=TaskSkill.FRONTEND,
        max_messages=1,
        auto_deliver=True,
        tasks_client=tasks_client,
        consumer=consumer,
    )

    assert claims == 1
    tasks_client.claim_task.assert_called_once()
    tasks_client.deliver_task.assert_called_once()
