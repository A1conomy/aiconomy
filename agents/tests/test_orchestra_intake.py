"""Tests for mock task intake agent."""

from simulation.orchestra.mock.intake import MockTaskIntakeAgent
from simulation.orchestra.models import UserTaskRequest


def test_intake_accepts_feasible_task() -> None:
    agent = MockTaskIntakeAgent()
    result = agent.validate(
        UserTaskRequest(
            title="Landing page",
            description="Responsive landing page with signup form and REST API backend integration.",
        )
    )
    assert result.accepted is True
    assert result.estimated_cost is not None
    assert len(result.required_skills) >= 2


def test_intake_rejects_out_of_scope() -> None:
    agent = MockTaskIntakeAgent()
    result = agent.validate(
        UserTaskRequest(
            title="Quantum teleport",
            description="Build a quantum teleportation device for office commute.",
        )
    )
    assert result.accepted is False
    assert "outside supported skills" in result.reason.lower() or "no agents" in result.reason.lower()


def test_intake_rejects_vague_task() -> None:
    agent = MockTaskIntakeAgent()
    result = agent.validate(UserTaskRequest(title="Hi", description="Do stuff"))
    assert result.accepted is False
