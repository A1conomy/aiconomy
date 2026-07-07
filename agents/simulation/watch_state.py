"""Shared live state for the visual mock demo dashboard."""

from __future__ import annotations

import threading
from dataclasses import dataclass, field
from datetime import UTC, datetime
from enum import Enum
from uuid import UUID


class SimulationPhase(str, Enum):
    BOOTING = "booting"
    RUNNING = "running"
    COMPLETE = "complete"
    FAILED = "failed"


@dataclass
class SimulationWatchState:
    """Thread-safe snapshot updated by the simulation runner for the dashboard."""

    run_id: str
    phase: SimulationPhase = SimulationPhase.BOOTING
    client_agent_id: str = ""
    manager_agent_id: str = ""
    task_ids: list[UUID] = field(default_factory=list)
    account_ids: dict[str, UUID] = field(default_factory=dict)
    events: list[str] = field(default_factory=list)
    error_message: str | None = None
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def set_phase(self, phase: SimulationPhase) -> None:
        with self._lock:
            self.phase = phase

    def register_accounts(self, accounts: dict[str, UUID]) -> None:
        with self._lock:
            self.account_ids.update(accounts)

    def register_tasks(self, task_ids: list[UUID]) -> None:
        with self._lock:
            self.task_ids = list(task_ids)

    def append_event(self, message: str) -> None:
        stamp = datetime.now(UTC).strftime("%H:%M:%S")
        line = f"[{stamp}] {message}"
        with self._lock:
            self.events.append(line)
            if len(self.events) > 30:
                self.events = self.events[-30:]

    def fail(self, message: str) -> None:
        with self._lock:
            self.phase = SimulationPhase.FAILED
            self.error_message = message
            self.append_event(f"FAILED: {message}")

    def snapshot_meta(self) -> dict[str, object]:
        """Return run metadata safe to merge with polled API data."""
        with self._lock:
            return {
                "runId": self.run_id,
                "phase": self.phase.value,
                "clientAgentId": self.client_agent_id,
                "managerAgentId": self.manager_agent_id,
                "taskIds": [str(task_id) for task_id in self.task_ids],
                "accountIds": {owner: str(account_id) for owner, account_id in self.account_ids.items()},
                "events": list(self.events),
                "errorMessage": self.error_message,
            }
