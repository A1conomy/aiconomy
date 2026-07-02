"""Worker agent — claims tasks and submits deliveries."""

from decimal import Decimal
from uuid import UUID

from common.clients.tasks import TasksClient
from common.events import TaskSkill
from worker_agent.decision import should_claim_task


def claim_matching_task(
    *,
    worker_agent_id: str,
    worker_account_id: UUID,
    worker_skill: TaskSkill,
    tasks_client: TasksClient | None = None,
) -> dict | None:
    """Claims the first open task matching the worker skill and minimum budget."""
    owned_tasks = tasks_client or TasksClient()
    try:
        for task in owned_tasks.list_open_tasks():
            required_skill = TaskSkill(task["requiredSkill"])
            budget = Decimal(str(task["budget"]))
            if should_claim_task(required_skill=required_skill, worker_skill=worker_skill, budget=budget):
                return owned_tasks.claim_task(
                    UUID(task["id"]),
                    agent_id=worker_agent_id,
                    agent_account_id=worker_account_id,
                )
        return None
    finally:
        if tasks_client is None:
            owned_tasks.close()


def deliver_task(
    *,
    task_id: UUID,
    worker_agent_id: str,
    deliverable_notes: str,
    tasks_client: TasksClient | None = None,
) -> dict:
    """Submits work for client review."""
    owned_tasks = tasks_client or TasksClient()
    try:
        return owned_tasks.deliver_task(
            task_id,
            agent_id=worker_agent_id,
            deliverable_notes=deliverable_notes,
        )
    finally:
        if tasks_client is None:
            owned_tasks.close()
