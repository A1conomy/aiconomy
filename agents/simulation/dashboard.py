"""Live dashboard HTTP server for the mock simulation demo."""

from __future__ import annotations

import json
import threading
from decimal import Decimal
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Callable
from uuid import UUID

from common.clients.ledger import LedgerClient
from common.clients.tasks import TasksClient
from simulation.watch_state import SimulationWatchState

_DASHBOARD_HTML = (Path(__file__).parent / "static" / "dashboard.html").read_text(encoding="utf-8")


def build_live_snapshot(watch_state: SimulationWatchState) -> dict[str, object]:
    """Poll tasks and ledger; merge with watch-state metadata."""
    meta = watch_state.snapshot_meta()
    task_ids = [UUID(task_id) for task_id in meta["taskIds"]]  # type: ignore[index]
    account_map: dict[str, str] = meta["accountIds"]  # type: ignore[assignment]

    tasks: list[dict[str, object]] = []
    accounts: list[dict[str, object]] = []
    accepted = 0

    with TasksClient() as tasks_client, LedgerClient() as ledger:
        for task_id in task_ids:
            try:
                task = tasks_client.get_task(task_id)
            except Exception:
                continue
            status = task.get("status", "UNKNOWN")
            if status == "ACCEPTED":
                accepted += 1
            tasks.append(
                {
                    "id": task.get("id"),
                    "title": task.get("title"),
                    "requiredSkill": task.get("requiredSkill"),
                    "budget": task.get("budget"),
                    "status": status,
                }
            )

        for owner_id, account_id_str in sorted(account_map.items()):
            try:
                account = ledger.get_account(UUID(account_id_str))
            except Exception:
                continue
            accounts.append(
                {
                    "ownerId": owner_id,
                    "accountType": account.get("accountType"),
                    "balance": account.get("balance"),
                }
            )

    total = len(task_ids)
    client_balance = next(
        (Decimal(str(row["balance"])) for row in accounts if row.get("ownerId") == meta.get("clientAgentId")),
        Decimal("0"),
    )
    worker_paid = sum(
        Decimal(str(row["balance"]))
        for row in accounts
        if str(row.get("accountType")) == "WORKER"
    )

    return {
        **meta,
        "tasks": tasks,
        "accounts": accounts,
        "progress": {"accepted": accepted, "total": total},
        "summary": {
            "clientBalance": str(client_balance),
            "workerPaidTotal": str(worker_paid),
        },
    }


def start_dashboard_server(
    watch_state: SimulationWatchState,
    *,
    host: str = "127.0.0.1",
    port: int = 8765,
) -> ThreadingHTTPServer:
    """Start background HTTP server; returns server instance (call shutdown() to stop)."""

    def snapshot_provider() -> dict[str, object]:
        return build_live_snapshot(watch_state)

    handler = _make_handler(snapshot_provider)
    server = ThreadingHTTPServer((host, port), handler)
    thread = threading.Thread(target=server.serve_forever, name="demo-dashboard", daemon=True)
    thread.start()
    return server


def _make_handler(snapshot_provider: Callable[[], dict[str, object]]):
    class DashboardHandler(BaseHTTPRequestHandler):
        def log_message(self, format: str, *args: object) -> None:
            return

        def do_GET(self) -> None:
            if self.path in ("/", "/index.html"):
                body = _DASHBOARD_HTML.encode("utf-8")
                self._respond(200, "text/html; charset=utf-8", body)
                return

            if self.path == "/api/state":
                payload = json.dumps(snapshot_provider()).encode("utf-8")
                self._respond(200, "application/json", payload)
                return

            self._respond(404, "text/plain", b"Not found")

        def _respond(self, status: int, content_type: str, body: bytes) -> None:
            self.send_response(status)
            self.send_header("Content-Type", content_type)
            self.send_header("Content-Length", str(len(body)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(body)

    return DashboardHandler
