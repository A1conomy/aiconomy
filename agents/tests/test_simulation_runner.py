"""Tests for mock simulation orchestrator."""

from decimal import Decimal
from unittest.mock import MagicMock, patch
from uuid import UUID

from common.events import TaskSkill
from common.mock_data import MOCK_PROJECTS
from simulation.runner import SimulationActors, WorkerProfile, post_mock_projects, run_mock_simulation


def _sample_actors() -> SimulationActors:
    return SimulationActors(
        client_agent_id="client-1",
        manager_agent_id="manager-1",
        client_account_id=UUID("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        manager_account_id=UUID("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
        workers=(
            WorkerProfile(
                agent_id="worker-fe",
                account_id=UUID("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                skill=TaskSkill.FRONTEND,
            ),
        ),
    )


@patch("simulation.runner.post_project_task")
def test_post_mock_projects_posts_all(mock_post: MagicMock) -> None:
    mock_post.return_value = {"id": "task-1"}
    actors = _sample_actors()

    posted = post_mock_projects(actors, projects=MOCK_PROJECTS[:1])

    assert len(posted) == 1
    mock_post.assert_called_once()


@patch("simulation.runner.LedgerClient")
@patch("simulation.runner.run_tasks_delivered_loop", return_value=1)
@patch("simulation.runner.run_worker_loop", return_value=1)
@patch("simulation.runner.run_tasks_posted_loop", return_value=1)
@patch("simulation.runner.post_mock_projects")
@patch("simulation.runner.bootstrap_actors")
def test_run_mock_simulation_returns_summary(
    mock_bootstrap: MagicMock,
    mock_post: MagicMock,
    mock_manager: MagicMock,
    mock_worker: MagicMock,
    mock_client: MagicMock,
    mock_ledger_cls: MagicMock,
) -> None:
    mock_bootstrap.return_value = _sample_actors()
    mock_post.return_value = [{"id": "22222222-2222-2222-2222-222222222222"}]

    ledger = MagicMock()
    ledger.get_account.return_value = {"balance": "100.00"}
    ledger.__enter__ = MagicMock(return_value=ledger)
    ledger.__exit__ = MagicMock(return_value=False)
    mock_ledger_cls.return_value = ledger

    result = run_mock_simulation(startup_delay_seconds=0.01, thread_join_timeout_seconds=5.0)

    assert result.posted_tasks == len(MOCK_PROJECTS)
    assert result.negotiations == 1
    assert result.client_reviews == 1
    assert result.client_balance == Decimal("100.00")
