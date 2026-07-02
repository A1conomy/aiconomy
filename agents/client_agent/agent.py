"""Client agent — posts projects and reviews deliveries."""

from decimal import Decimal
from uuid import UUID, uuid4

from common.clients.tasks import TasksClient
from common.events import TaskSkill
from client_agent.decision import should_accept_delivery


def post_project_task(
    *,
    client_agent_id: str,
    client_account_id: UUID,
    title: str,
    description: str,
    required_skill: TaskSkill,
    budget: Decimal,
    project_id: UUID | None = None,
    tasks_client: TasksClient | None = None,
) -> dict:
    """Posts a task to the board via REST (task service publishes tasks.posted)."""
    owned_tasks = tasks_client or TasksClient()
    try:
        return owned_tasks.post_task(
            project_id=project_id or uuid4(),
            title=title,
            description=description,
            required_skill=required_skill,
            budget=budget,
            client_agent_id=client_agent_id,
            client_account_id=client_account_id,
        )
    finally:
        if tasks_client is None:
            owned_tasks.close()


def review_delivery(
    *,
    task_id: UUID,
    client_agent_id: str,
    deliverable_notes: str,
    budget: Decimal,
    tasks_client: TasksClient | None = None,
) -> dict:
    """Accepts or rejects a delivered task based on deterministic rules."""
    owned_tasks = tasks_client or TasksClient()
    try:
        if should_accept_delivery(deliverable_notes=deliverable_notes, budget=budget):
            return owned_tasks.accept_task(task_id, client_agent_id=client_agent_id)
        return owned_tasks.reject_task(
            task_id,
            client_agent_id=client_agent_id,
            reason="Delivery does not meet acceptance criteria",
        )
    finally:
        if tasks_client is None:
            owned_tasks.close()
