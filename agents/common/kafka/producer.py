"""Kafka producer helpers for agent events."""

from __future__ import annotations

import json
from typing import Any

from confluent_kafka import Producer

from common.config import AgentConfig, load_config
from common.events import PaymentAcceptedEvent, PaymentProposedEvent, TaskPostedEvent
from common.kafka.topics import PAYMENTS_ACCEPTED, PAYMENTS_PROPOSED, TASKS_POSTED


class AgentEventProducer:
    """Publishes agent-originated events to Kafka."""

    def __init__(self, config: AgentConfig | None = None, producer: Producer | None = None) -> None:
        self._config = config or load_config()
        self._producer = producer or Producer({"bootstrap.servers": self._config.kafka_bootstrap_servers})
        self._owns_producer = producer is None

    def publish_task_posted(self, event: TaskPostedEvent) -> None:
        self._publish(TASKS_POSTED, str(event.task_id), event.to_json_dict())

    def publish_payment_proposed(self, event: PaymentProposedEvent) -> None:
        self._publish(PAYMENTS_PROPOSED, str(event.task_id), event.to_json_dict())

    def publish_payment_accepted(self, event: PaymentAcceptedEvent) -> None:
        self._publish(PAYMENTS_ACCEPTED, str(event.task_id), event.to_json_dict())

    def _publish(self, topic: str, key: str, payload: dict[str, Any]) -> None:
        self._producer.produce(topic, key=key, value=json.dumps(payload))
        self._producer.flush()

    def close(self) -> None:
        if self._owns_producer:
            self._producer.flush()

    def __enter__(self) -> AgentEventProducer:
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
