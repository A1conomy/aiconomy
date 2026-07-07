"""Deterministic mock projects for local simulation (no LLM)."""

from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

from common.events import TaskSkill


@dataclass(frozen=True)
class MockProject:
    title: str
    description: str
    required_skill: TaskSkill
    budget: Decimal


MOCK_PROJECTS: tuple[MockProject, ...] = (
    MockProject(
        title="Landing page for product launch",
        description="Responsive landing page with hero section and signup form",
        required_skill=TaskSkill.FRONTEND,
        budget=Decimal("400.00"),
    ),
    MockProject(
        title="REST API for user profiles",
        description="Spring Boot endpoints with validation and Postgres persistence",
        required_skill=TaskSkill.BACKEND,
        budget=Decimal("600.00"),
    ),
    MockProject(
        title="Brand style guide",
        description="Color palette, typography, and component mockups for marketing site",
        required_skill=TaskSkill.DESIGN,
        budget=Decimal("250.00"),
    ),
)
