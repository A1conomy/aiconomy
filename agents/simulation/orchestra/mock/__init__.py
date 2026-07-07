"""Mock implementations for the orchestration demo."""

from simulation.orchestra.mock.execution import MockExecutionSimulator
from simulation.orchestra.mock.intake import MockTaskIntakeAgent
from simulation.orchestra.mock.managers import MockManagerPool
from simulation.orchestra.mock.review import MockClientReviewAgent
from simulation.orchestra.mock.team import MockTeamAssembler

__all__ = [
    "MockClientReviewAgent",
    "MockExecutionSimulator",
    "MockManagerPool",
    "MockTaskIntakeAgent",
    "MockTeamAssembler",
]
