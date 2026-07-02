"""Manager agent — negotiates with clients and orchestrates workers."""

from datetime import UTC, datetime
from decimal import Decimal
from uuid import UUID, uuid4

from common.events import PaymentAcceptedEvent, PaymentProposedEvent
from common.kafka.producer import AgentEventProducer
from manager_agent.decision import propose_project_fee


def negotiate_with_client(
    *,
    task_id: UUID,
    client_agent_id: str,
    manager_agent_id: str,
    budget: Decimal,
    producer: AgentEventProducer | None = None,
) -> PaymentAcceptedEvent:
    """Propose a manager fee on Kafka and immediately accept it (demo shortcut)."""
    amount = propose_project_fee(budget=budget)
    owned_producer = producer or AgentEventProducer()
    try:
        proposal = PaymentProposedEvent(
            event_id=uuid4(),
            task_id=task_id,
            from_agent_id=manager_agent_id,
            to_agent_id=client_agent_id,
            amount=amount,
            rationale=f"Project management and worker coordination for task {task_id}",
            proposed_at=datetime.now(UTC),
        )
        owned_producer.publish_payment_proposed(proposal)

        acceptance = PaymentAcceptedEvent(
            event_id=uuid4(),
            task_id=task_id,
            from_agent_id=client_agent_id,
            to_agent_id=manager_agent_id,
            amount=amount,
            accepted_at=datetime.now(UTC),
        )
        owned_producer.publish_payment_accepted(acceptance)
        return acceptance
    finally:
        if producer is None:
            owned_producer.close()
