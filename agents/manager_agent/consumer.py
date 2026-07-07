"""Manager agent Kafka loop — negotiates fees when new tasks are posted."""

from __future__ import annotations

import logging

from common.events import TaskPostedEvent
from common.kafka.consumer import AgentEventConsumer
from common.kafka.producer import AgentEventProducer
from common.kafka.topics import TASKS_POSTED
from manager_agent.agent import negotiate_with_client

logger = logging.getLogger(__name__)


def handle_task_posted(
    event: TaskPostedEvent,
    *,
    manager_agent_id: str,
    client_agent_id: str,
    producer: AgentEventProducer,
) -> bool:
    """Negotiate manager fee when the task belongs to the configured client."""
    if event.client_agent_id != client_agent_id:
        logger.info("Skipping task %s — client %s", event.task_id, event.client_agent_id)
        return False

    acceptance = negotiate_with_client(
        task_id=event.task_id,
        client_agent_id=client_agent_id,
        manager_agent_id=manager_agent_id,
        budget=event.budget,
        producer=producer,
    )
    logger.info(
        "Negotiated manager fee %s for task %s",
        acceptance.amount,
        event.task_id,
    )
    return True


def run_tasks_posted_loop(
    *,
    manager_agent_id: str,
    client_agent_id: str,
    max_messages: int | None = None,
    producer: AgentEventProducer | None = None,
    consumer: AgentEventConsumer | None = None,
    consumer_group_suffix: str | None = None,
) -> int:
    """Listen on tasks.posted and negotiate manager fees for the simulation client."""
    group_id = f"manager-{manager_agent_id}"
    if consumer_group_suffix:
        group_id = f"{group_id}-{consumer_group_suffix}"

    owned_producer = producer or AgentEventProducer()
    owned_consumer = consumer or AgentEventConsumer(
        group_id=group_id,
        topics=[TASKS_POSTED],
        auto_offset_reset="latest" if consumer_group_suffix else "earliest",
    )

    def on_message(topic: str, payload: dict) -> bool:
        if topic != TASKS_POSTED:
            return False

        event = TaskPostedEvent.from_json_dict(payload)
        return handle_task_posted(
            event,
            manager_agent_id=manager_agent_id,
            client_agent_id=client_agent_id,
            producer=owned_producer,
        )

    try:
        if max_messages is not None and consumer_group_suffix:
            return owned_consumer.consume_while(on_message, until_successes=max_messages)
        return owned_consumer.consume_while(on_message, max_messages=max_messages)
    finally:
        if consumer is None:
            owned_consumer.close()
        if producer is None:
            owned_producer.close()
