"""Tests for live demo dashboard snapshot builder."""

from decimal import Decimal
from unittest.mock import MagicMock, patch
from uuid import UUID

from simulation.dashboard import build_live_snapshot
from simulation.watch_state import SimulationPhase, SimulationWatchState


def test_build_live_snapshot_merges_api_data() -> None:
    task_id = UUID("22222222-2222-2222-2222-222222222222")
    client_account = UUID("44444444-4444-4444-4444-444444444444")
    worker_account = UUID("55555555-5555-5555-5555-555555555555")

    watch = SimulationWatchState(run_id="abc123")
    watch.client_agent_id = "sim-client"
    watch.set_phase(SimulationPhase.RUNNING)
    watch.register_tasks([task_id])
    watch.register_accounts({"sim-client": client_account, "sim-worker-frontend": worker_account})
    watch.append_event("Posted 1 mock project")

    tasks_client = MagicMock()
    tasks_client.get_task.return_value = {
        "id": str(task_id),
        "title": "Landing page",
        "requiredSkill": "FRONTEND",
        "budget": 400.0,
        "status": "ACCEPTED",
    }
    tasks_client.__enter__.return_value = tasks_client

    ledger = MagicMock()
    ledger.get_account.side_effect = [
        {"ownerId": "sim-client", "accountType": "CLIENT", "balance": "3750.00"},
        {"ownerId": "sim-worker-frontend", "accountType": "WORKER", "balance": "400.00"},
    ]
    ledger.__enter__.return_value = ledger

    with patch("simulation.dashboard.TasksClient", return_value=tasks_client), patch(
        "simulation.dashboard.LedgerClient", return_value=ledger
    ):
        snapshot = build_live_snapshot(watch)

    assert snapshot["phase"] == "running"
    assert snapshot["progress"] == {"accepted": 1, "total": 1}
    assert snapshot["tasks"][0]["status"] == "ACCEPTED"
    assert snapshot["summary"]["clientBalance"] == "3750.00"
    assert snapshot["summary"]["workerPaidTotal"] == str(Decimal("400.00"))
    assert "Posted 1 mock project" in snapshot["events"][0]
