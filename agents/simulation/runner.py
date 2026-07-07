"""Orchestrates a multi-agent mock simulation over Kafka."""

from __future__ import annotations

import logging
import threading
import time
from dataclasses import dataclass
from decimal import Decimal
from uuid import UUID, uuid4

from client_agent.agent import post_project_task
from client_agent.consumer import run_tasks_delivered_loop
from common.clients.ledger import LedgerClient
from common.events import TaskSkill
from common.mock_data import MOCK_PROJECTS, MockProject
from manager_agent.consumer import run_tasks_posted_loop
from simulation.watch_state import SimulationPhase, SimulationWatchState
from worker_agent.consumer import run_tasks_posted_loop as run_worker_loop

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class WorkerProfile:
    agent_id: str
    account_id: UUID
    skill: TaskSkill


@dataclass(frozen=True)
class SimulationActors:
    client_agent_id: str
    manager_agent_id: str
    client_account_id: UUID
    manager_account_id: UUID
    workers: tuple[WorkerProfile, ...]


@dataclass(frozen=True)
class SimulationResult:
    posted_tasks: int
    negotiations: int
    worker_completions: int
    client_reviews: int
    client_balance: Decimal
    worker_balances: dict[str, Decimal]
    task_ids: tuple[UUID, ...] = ()


def bootstrap_actors(
    *,
    client_agent_id: str = "sim-client",
    manager_agent_id: str = "sim-manager",
    initial_client_balance: Decimal = Decimal("5000.00"),
) -> SimulationActors:
    """Create ledger accounts for one client, manager, and one worker per mock skill."""
    skills = {project.required_skill for project in MOCK_PROJECTS}
    with LedgerClient() as ledger:
        client_account_id = ledger.create_account(client_agent_id, "CLIENT", initial_client_balance)
        manager_account_id = ledger.create_account(manager_agent_id, "MANAGER", Decimal("0.00"))
        workers: list[WorkerProfile] = []
        for skill in sorted(skills, key=lambda s: s.value):
            worker_id = f"sim-worker-{skill.value.lower()}"
            account_id = ledger.create_account(worker_id, "WORKER", Decimal("0.00"))
            workers.append(WorkerProfile(agent_id=worker_id, account_id=account_id, skill=skill))

    return SimulationActors(
        client_agent_id=client_agent_id,
        manager_agent_id=manager_agent_id,
        client_account_id=client_account_id,
        manager_account_id=manager_account_id,
        workers=tuple(workers),
    )


def post_mock_projects(
    actors: SimulationActors,
    projects: tuple[MockProject, ...] = MOCK_PROJECTS,
) -> list[dict]:
    """Post all mock projects to the task board."""
    posted: list[dict] = []
    project_id = uuid4()
    for project in projects:
        task = post_project_task(
            client_agent_id=actors.client_agent_id,
            client_account_id=actors.client_account_id,
            project_id=project_id,
            title=project.title,
            description=project.description,
            required_skill=project.required_skill,
            budget=project.budget,
        )
        posted.append(task)
        logger.info("Posted task %s skill=%s budget=%s", task["id"], project.required_skill.value, project.budget)
    return posted


def run_mock_simulation(
    *,
    actors: SimulationActors | None = None,
    projects: tuple[MockProject, ...] = MOCK_PROJECTS,
    startup_delay_seconds: float = 2.0,
    thread_join_timeout_seconds: float = 60.0,
    watch_state: SimulationWatchState | None = None,
) -> SimulationResult:
    """Run manager, workers, and client consumers; inject mock tasks; return summary."""
    simulation_actors = actors or bootstrap_actors()
    message_budget = len(projects)
    run_id = uuid4().hex[:8]
    results: dict[str, int] = {"negotiations": 0, "worker_completions": 0, "client_reviews": 0}
    threads: list[threading.Thread] = []

    if watch_state is not None:
        watch_state.client_agent_id = simulation_actors.client_agent_id
        watch_state.manager_agent_id = simulation_actors.manager_agent_id
        watch_state.register_accounts(
            {
                simulation_actors.client_agent_id: simulation_actors.client_account_id,
                simulation_actors.manager_agent_id: simulation_actors.manager_account_id,
                **{w.agent_id: w.account_id for w in simulation_actors.workers},
            }
        )
        watch_state.append_event("Bootstrapped ledger accounts")
        watch_state.set_phase(SimulationPhase.BOOTING)

    def manager_thread() -> None:
        results["negotiations"] = run_tasks_posted_loop(
            manager_agent_id=simulation_actors.manager_agent_id,
            client_agent_id=simulation_actors.client_agent_id,
            max_messages=message_budget,
            consumer_group_suffix=run_id,
        )

    def client_thread() -> None:
        results["client_reviews"] = run_tasks_delivered_loop(
            client_agent_id=simulation_actors.client_agent_id,
            max_messages=message_budget,
            consumer_group_suffix=run_id,
        )

    threads.append(threading.Thread(target=manager_thread, name="manager-consumer"))
    threads.append(threading.Thread(target=client_thread, name="client-consumer"))

    for worker in simulation_actors.workers:
        def worker_thread(profile: WorkerProfile = worker) -> None:
            completed = run_worker_loop(
                worker_agent_id=profile.agent_id,
                worker_account_id=profile.account_id,
                worker_skill=profile.skill,
                max_messages=1,
                auto_deliver=True,
                consumer_group_suffix=run_id,
            )
            results[f"worker_{profile.skill.value}"] = completed

        threads.append(threading.Thread(target=worker_thread, name=f"worker-{worker.skill.value}"))

    for thread in threads:
        thread.start()

    if watch_state is not None:
        watch_state.append_event("Kafka consumers started")
        watch_state.set_phase(SimulationPhase.RUNNING)

    time.sleep(startup_delay_seconds)
    posted = post_mock_projects(simulation_actors, projects)
    posted_ids = [UUID(str(task["id"])) for task in posted]

    if watch_state is not None:
        watch_state.register_tasks(posted_ids)
        watch_state.append_event(f"Posted {len(posted)} mock projects")

    for thread in threads:
        thread.join(timeout=thread_join_timeout_seconds)

    worker_completions = sum(
        results.get(f"worker_{worker.skill.value}", 0) for worker in simulation_actors.workers
    )

    with LedgerClient() as ledger:
        client_balance = Decimal(str(ledger.get_account(simulation_actors.client_account_id)["balance"]))
        worker_balances = {
            worker.agent_id: Decimal(str(ledger.get_account(worker.account_id)["balance"]))
            for worker in simulation_actors.workers
        }

    result = SimulationResult(
        posted_tasks=len(projects),
        negotiations=results["negotiations"],
        worker_completions=worker_completions,
        client_reviews=results["client_reviews"],
        client_balance=client_balance,
        worker_balances=worker_balances,
        task_ids=tuple(posted_ids),
    )

    if watch_state is not None:
        if result.client_reviews < result.posted_tasks:
            watch_state.fail("Not all tasks were accepted")
        else:
            watch_state.append_event(
                f"Complete — client balance {client_balance}, {worker_completions} workers paid"
            )
            watch_state.set_phase(SimulationPhase.COMPLETE)

    return result
