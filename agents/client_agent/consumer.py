"""Client agent Kafka loop — accepts or rejects delivered tasks."""

from __future__ import annotations

import logging
from decimal import Decimal

from common.clients.tasks import TasksClient
from common.events import TaskDeliveredEvent
from common.kafka.consumer import AgentEventConsumer
from common.kafka.topics import TASKS_DELIVERED
from client_agent.agent import review_delivery

logger = logging.getLogger(__name__)


def handle_task_delivered(
    event: TaskDeliveredEvent,
    *,
    client_agent_id: str,
    tasks_client: TasksClient,
) -> dict | None:
    """Review delivery when this client owns the task."""
    task = tasks_client.get_task(event.task_id)
    if task["clientAgentId"] != client_agent_id:
        logger.info("Skipping task %s — not owned by client %s", event.task_id, client_agent_id)
        return None

    result = review_delivery(
        task_id=event.task_id,
        client_agent_id=client_agent_id,
        deliverable_notes=event.deliverable_notes,
        budget=Decimal(str(task["budget"])),
        tasks_client=tasks_client,
    )
    logger.info("Client review for task %s: %s", event.task_id, result["status"])
    return result


def run_tasks_delivered_loop(
    *,
    client_agent_id: str,
    max_messages: int | None = None,
    tasks_client: TasksClient | None = None,
    consumer: AgentEventConsumer | None = None,
    consumer_group_suffix: str | None = None,
) -> int:
    """Listen on tasks.delivered and accept or reject with mock rules."""
    group_id = f"client-{client_agent_id}"
    if consumer_group_suffix:
        group_id = f"{group_id}-{consumer_group_suffix}"

    owned_tasks = tasks_client or TasksClient()
    owned_consumer = consumer or AgentEventConsumer(
        group_id=group_id,
        topics=[TASKS_DELIVERED],
        auto_offset_reset="latest" if consumer_group_suffix else "earliest",
    )

    def on_message(topic: str, payload: dict) -> bool:
        if topic != TASKS_DELIVERED:
            return False

        event = TaskDeliveredEvent.from_json_dict(payload)
        return handle_task_delivered(
            event,
            client_agent_id=client_agent_id,
            tasks_client=owned_tasks,
        ) is not None

    try:
        if max_messages is not None and consumer_group_suffix:
            return owned_consumer.consume_while(on_message, until_successes=max_messages)
        return owned_consumer.consume_while(on_message, max_messages=max_messages)
    finally:
        if consumer is None:
            owned_consumer.close()
        if tasks_client is None:
            owned_tasks.close()
