"""Tests for staged orchestration runner."""

from simulation.orchestra.models import OrchestrationPhase, UserTaskRequest
from simulation.orchestra.runner import run_orchestration
from simulation.orchestra.state import OrchestrationWatchState


def test_orchestration_runner_completes_fast() -> None:
    watch = OrchestrationWatchState(run_id="test-run")
    request = UserTaskRequest(
        title="Landing page",
        description="Responsive landing page with signup form and REST API integration.",
    )
    result = run_orchestration(
        request,
        watch_state=watch,
        step_delay_seconds=0,
        bid_delay_seconds=0,
        message_delay_seconds=0,
    )
    assert result.phase == OrchestrationPhase.COMPLETE
    assert result.validation.accepted is True
    assert result.winning_bid is not None
    assert result.team_plan is not None
    assert result.client_review is not None
    assert result.client_review.accepted is True
    assert result.final_balances is not None
    assert result.final_balances.client_balance < 5000


def test_orchestration_rejects_invalid_task() -> None:
    watch = OrchestrationWatchState(run_id="reject-run")
    result = run_orchestration(
        UserTaskRequest(title="Quantum app", description="Quantum impossible teleport system."),
        watch_state=watch,
        step_delay_seconds=0,
        bid_delay_seconds=0,
        message_delay_seconds=0,
    )
    assert result.phase == OrchestrationPhase.REJECTED
    assert watch.phase == OrchestrationPhase.REJECTED
