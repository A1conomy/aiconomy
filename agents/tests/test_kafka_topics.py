"""Tests for Kafka topic constants."""

from common.kafka import topics


def test_task_topics_match_java_constants() -> None:
    assert topics.TASKS_POSTED == "tasks.posted"
    assert topics.PAYMENTS_PROPOSED == "payments.proposed"
    assert topics.SIMULATION_TICK == "simulation.tick"
