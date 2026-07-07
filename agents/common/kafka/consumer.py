"""Kafka consumer helpers for agent event loops."""

from __future__ import annotations

import json
from collections.abc import Callable
from typing import Any

from confluent_kafka import Consumer, KafkaError, Message

from common.config import AgentConfig, load_config


class AgentEventConsumer:
    """Subscribes to Kafka topics and dispatches parsed JSON payloads."""

    def __init__(
        self,
        group_id: str,
        topics: list[str],
        config: AgentConfig | None = None,
        consumer: Consumer | None = None,
        auto_offset_reset: str = "earliest",
    ) -> None:
        self._config = config or load_config()
        self._owns_consumer = consumer is None
        self._consumer = consumer or Consumer(
            {
                "bootstrap.servers": self._config.kafka_bootstrap_servers,
                "group.id": group_id,
                "auto.offset.reset": auto_offset_reset,
                "enable.auto.commit": True,
            }
        )
        self._consumer.subscribe(topics)

    def poll_json(self, timeout: float = 1.0) -> tuple[str, dict[str, Any]] | None:
        """Returns (topic, payload) for the next message, or None on timeout."""
        message = self._consumer.poll(timeout)
        if message is None:
            return None
        if message.error():
            if message.error().code() == KafkaError._PARTITION_EOF:
                return None
            raise RuntimeError(f"Kafka consumer error: {message.error()}")

        topic = message.topic()
        payload = json.loads(message.value().decode("utf-8"))
        return topic, payload

    def consume_while(
        self,
        handler: Callable[[str, dict[str, Any]], bool],
        *,
        max_messages: int | None = None,
        until_successes: int | None = None,
        poll_timeout: float = 1.0,
    ) -> int:
        """Handler returns True when an action succeeded (counts toward until_successes)."""
        messages = 0
        successes = 0

        while True:
            if max_messages is not None and messages >= max_messages:
                break
            if until_successes is not None and successes >= until_successes:
                break
            if max_messages is None and until_successes is None:
                break

            result = self.poll_json(poll_timeout)
            if result is None:
                continue

            messages += 1
            topic, payload = result
            if handler(topic, payload):
                successes += 1

        return successes if until_successes is not None else messages

    def close(self) -> None:
        if self._owns_consumer:
            self._consumer.close()

    def __enter__(self) -> AgentEventConsumer:
        return self

    def __exit__(self, *args: object) -> None:
        self.close()
