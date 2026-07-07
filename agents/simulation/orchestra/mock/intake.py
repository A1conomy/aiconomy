"""Hardcoded task intake — swap with LLM-backed validator later."""

from __future__ import annotations

import re
from decimal import Decimal

from common.events import TaskSkill
from simulation.orchestra.models import UserTaskRequest, ValidationResult
from simulation.orchestra.survival import estimate_task_duration_days, minimum_task_budget

_REJECT_PATTERNS = (
    r"\bquantum\b",
    r"\bimpossible\b",
    r"\bteleport\b",
    r"\btime travel\b",
    r"\bperpetual motion\b",
)

_SKILL_KEYWORDS: dict[TaskSkill, tuple[str, ...]] = {
    TaskSkill.FRONTEND: ("landing", "frontend", "react", "page", "ui", "responsive", "signup", "form"),
    TaskSkill.BACKEND: ("api", "backend", "spring", "rest", "database", "postgres", "endpoint", "server"),
    TaskSkill.DESIGN: ("design", "wireframe", "brand", "style guide", "mockup", "figma", "visual"),
    TaskSkill.COPYWRITING: ("copy", "copywriting", "headline", "marketing text", "blog post"),
}


class MockTaskIntakeAgent:
    """Rule-based gatekeeper that estimates scope and cost."""

    def validate(self, request: UserTaskRequest) -> ValidationResult:
        text = f"{request.title} {request.description}".strip().lower()
        if len(text) < 12:
            return ValidationResult(
                accepted=False,
                reason="Task description is too vague — add concrete deliverables.",
            )

        for pattern in _REJECT_PATTERNS:
            if re.search(pattern, text):
                return ValidationResult(
                    accepted=False,
                    reason="No agents in the pool can deliver this scope (outside supported skills).",
                )

        skills = self._detect_skills(text)
        if not skills:
            return ValidationResult(
                accepted=False,
                reason="Could not map requirements to available agent skills (frontend, backend, design).",
            )

        cost = self._estimate_cost(skills, text)
        duration_days = estimate_task_duration_days(skill_count=len(skills), description_length=len(text))
        team_size = len(skills) + 1  # specialists + manager
        floor = minimum_task_budget(duration_days, team_size)

        if cost < floor:
            return ValidationResult(
                accepted=False,
                reason=(
                    f"Budget too low for agent survival — need at least ${floor} "
                    f"for a {duration_days}-day job ({team_size} agents x $100/month upkeep)."
                ),
                estimated_cost=cost,
                required_skills=skills,
                estimated_duration_days=duration_days,
                survival_floor=floor,
            )

        skill_list = ", ".join(skill.value.lower() for skill in skills)
        return ValidationResult(
            accepted=True,
            reason=(
                f"Task is feasible. Required skills: {skill_list}. "
                f"Estimated {duration_days} sim-days; survival floor ${floor}."
            ),
            estimated_cost=cost,
            required_skills=skills,
            estimated_duration_days=duration_days,
            survival_floor=floor,
        )

    def _detect_skills(self, text: str) -> tuple[TaskSkill, ...]:
        matched: list[TaskSkill] = []
        for skill, keywords in _SKILL_KEYWORDS.items():
            if any(keyword in text for keyword in keywords):
                matched.append(skill)
        return tuple(matched)

    def _estimate_cost(self, skills: tuple[TaskSkill, ...], text: str) -> Decimal:
        if len(text) < 45:
            return Decimal("40.00")
        base = Decimal("180.00")
        per_skill = Decimal("220.00") * len(skills)
        complexity = Decimal(str(min(len(text) // 40, 8))) * Decimal("25.00")
        return (base + per_skill + complexity).quantize(Decimal("0.01"))
