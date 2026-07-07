"""Worker agent Kafka loop — reacts to tasks.posted and claims matching work."""

from __future__ import annotations

import logging
from uuid import UUID

import httpx

from common.clients.tasks import TasksClient
from common.events import TaskPostedEvent, TaskSkill
from common.kafka.consumer import AgentEventConsumer
from common.kafka.topics import TASKS_POSTED
from worker_agent.agent import deliver_task
from worker_agent.decision import mock_deliverable_notes, should_claim_task

logger = logging.getLogger(__name__)


def handle_task_posted(
    event: TaskPostedEvent,
    *,
    worker_agent_id: str,
    worker_account_id: UUID,
    worker_skill: TaskSkill,
    tasks_client: TasksClient,
    auto_deliver: bool = True,
) -> dict | None:
    """Claim the task when skill and budget rules pass; optionally auto-deliver for simulation."""
    if not should_claim_task(
        required_skill=event.required_skill,
        worker_skill=worker_skill,
        budget=event.budget,
    ):
        logger.info(
            "Skipping task %s — skill=%s budget=%s",
            event.task_id,
            event.required_skill.value,
            event.budget,
        )
        return None

    try:
        claimed = tasks_client.claim_task(
            event.task_id,
            agent_id=worker_agent_id,
            agent_account_id=worker_account_id,
        )
    except httpx.HTTPStatusError as ex:
        if ex.response.status_code == 409:
            logger.info("Task %s already claimed by another agent", event.task_id)
            return None
        raise

    logger.info("Claimed task %s for worker %s", event.task_id, worker_agent_id)

    if auto_deliver:
        delivered = deliver_task(
            task_id=event.task_id,
            worker_agent_id=worker_agent_id,
            deliverable_notes=mock_deliverable_notes(title=event.title),
            tasks_client=tasks_client,
        )
        logger.info("Delivered task %s status=%s", event.task_id, delivered["status"])
        return delivered

    return claimed


def run_tasks_posted_loop(
    *,
    worker_agent_id: str,
    worker_account_id: UUID,
    worker_skill: TaskSkill,
    max_messages: int | None = None,
    auto_deliver: bool = True,
    tasks_client: TasksClient | None = None,
    consumer: AgentEventConsumer | None = None,
    consumer_group_suffix: str | None = None,
) -> int:
    """Listen on tasks.posted and claim tasks that match this worker profile."""
    group_id = f"worker-{worker_agent_id}"
    if consumer_group_suffix:
        group_id = f"{group_id}-{consumer_group_suffix}"

    owned_tasks = tasks_client or TasksClient()
    owned_consumer = consumer or AgentEventConsumer(
        group_id=group_id,
        topics=[TASKS_POSTED],
        auto_offset_reset="latest" if consumer_group_suffix else "earliest",
    )
    claims = 0

    def on_message(topic: str, payload: dict) -> bool:
        if topic != TASKS_POSTED:
            return False

        event = TaskPostedEvent.from_json_dict(payload)
        result = handle_task_posted(
            event,
            worker_agent_id=worker_agent_id,
            worker_account_id=worker_account_id,
            worker_skill=worker_skill,
            tasks_client=owned_tasks,
            auto_deliver=auto_deliver,
        )
        return result is not None

    try:
        if max_messages is not None and consumer_group_suffix:
            claims = owned_consumer.consume_while(on_message, until_successes=max_messages)
        else:
            claims = owned_consumer.consume_while(on_message, max_messages=max_messages)
    finally:
        if consumer is None:
            owned_consumer.close()
        if tasks_client is None:
            owned_tasks.close()

    return claims
