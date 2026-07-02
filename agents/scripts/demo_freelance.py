#!/usr/bin/env python3
"""End-to-end freelancing demo — client, manager, worker agents."""

from decimal import Decimal
from uuid import UUID

from client_agent.agent import post_project_task, review_delivery
from common.clients.ledger import LedgerClient
from common.events import TaskSkill
from manager_agent.agent import negotiate_with_client
from worker_agent.agent import claim_matching_task, deliver_task


def main() -> None:
    with LedgerClient() as ledger:
        client_account = ledger.create_account("demo-client", "CLIENT", Decimal("1000.00"))
        manager_account = ledger.create_account("demo-manager", "MANAGER", Decimal("0.00"))
        worker_account = ledger.create_account("demo-worker", "WORKER", Decimal("0.00"))

    print(f"Client account:  {client_account}")
    print(f"Manager account: {manager_account}")
    print(f"Worker account:  {worker_account}")

    task = post_project_task(
        client_agent_id="client-demo",
        client_account_id=client_account,
        title="Landing page for product launch",
        description="Responsive landing page with hero section and signup form",
        required_skill=TaskSkill.FRONTEND,
        budget=Decimal("400.00"),
    )
    task_id = UUID(task["id"])
    print(f"Task posted: {task_id} status={task['status']} budget={task['budget']}")

    negotiation = negotiate_with_client(
        task_id=task_id,
        client_agent_id="client-demo",
        manager_agent_id="manager-demo",
        budget=Decimal(str(task["budget"])),
    )
    print(f"Manager fee negotiated: {negotiation.amount} (client -> manager on Kafka)")

    claimed = claim_matching_task(
        worker_agent_id="worker-demo",
        worker_account_id=worker_account,
        worker_skill=TaskSkill.FRONTEND,
    )
    if claimed is None:
        raise RuntimeError("No matching task found for worker")
    print(f"Task claimed: {claimed['id']} escrow={claimed.get('escrowHoldId')}")

    delivered = deliver_task(
        task_id=task_id,
        worker_agent_id="worker-demo",
        deliverable_notes="Deployed landing page to staging URL",
    )
    print(f"Task delivered: {delivered['status']}")

    reviewed = review_delivery(
        task_id=task_id,
        client_agent_id="client-demo",
        deliverable_notes=delivered["deliverableNotes"],
        budget=Decimal(str(task["budget"])),
    )
    print(f"Client review: {reviewed['status']}")

    with LedgerClient() as ledger:
        client_balance = Decimal(str(ledger.get_account(client_account)["balance"]))
        worker_balance = Decimal(str(ledger.get_account(worker_account)["balance"]))

    print(f"Final balances — client: {client_balance}, worker: {worker_balance}")
    print("Demo complete.")


if __name__ == "__main__":
    main()
