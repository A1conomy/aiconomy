"""Mock team assembly — picks specialists and builds attack plan."""

from __future__ import annotations

from decimal import Decimal

from common.events import TaskSkill
from simulation.orchestra.mock.registry import SPECIALIST_POOL
from simulation.orchestra.models import ManagerBid, TeamPlan, UserTaskRequest, ValidationResult, WorkAssignment
from simulation.orchestra.survival import minimum_acceptable_payout


class MockTeamAssembler:
    """Builds a deterministic plan and price breakdown from required skills."""

    _SUBTASKS: dict[TaskSkill, str] = {
        TaskSkill.DESIGN: "Wireframes, visual hierarchy, and component specs",
        TaskSkill.FRONTEND: "Responsive UI implementation and form validation",
        TaskSkill.BACKEND: "REST endpoints, persistence, and integration tests",
        TaskSkill.COPYWRITING: "Hero copy, CTAs, and onboarding microcopy",
    }

    _SKILL_WEIGHTS: dict[TaskSkill, Decimal] = {
        TaskSkill.DESIGN: Decimal("0.20"),
        TaskSkill.FRONTEND: Decimal("0.35"),
        TaskSkill.BACKEND: Decimal("0.35"),
        TaskSkill.COPYWRITING: Decimal("0.10"),
    }

    def build_plan(
        self,
        request: UserTaskRequest,
        validation: ValidationResult,
        winning_bid: ManagerBid,
    ) -> TeamPlan:
        if validation.estimated_cost is None:
            raise ValueError("Cannot plan without estimated cost")

        total = validation.estimated_cost
        manager_fee = winning_bid.fee_amount
        worker_budget = (total - manager_fee).quantize(Decimal("0.01"))
        skills = validation.required_skills or (TaskSkill.FRONTEND,)
        duration_days = validation.estimated_duration_days or 7
        min_worker_payout = minimum_acceptable_payout(duration_days)

        weights = [self._SKILL_WEIGHTS.get(skill, Decimal("0.25")) for skill in skills]
        weight_sum = sum(weights, Decimal("0"))
        assignments: list[WorkAssignment] = []

        for skill, weight in zip(skills, weights, strict=True):
            pool = SPECIALIST_POOL.get(skill, ())
            if not pool:
                continue
            share = (worker_budget * weight / weight_sum).quantize(Decimal("0.01"))
            agent = self._pick_agent(pool, share, min_worker_payout)
            if agent is None:
                raise ValueError(
                    f"No {skill.value} agent accepts ${share} — survival minimum "
                    f"${min_worker_payout} for {duration_days} sim-days"
                )
            assignments.append(
                WorkAssignment(
                    agent=agent,
                    subtask=self._SUBTASKS.get(skill, f"{skill.value} deliverables"),
                    payout=share,
                )
            )

        attack_plan = self._attack_plan(request.title, skills)
        return TeamPlan(
            manager=winning_bid.manager,
            manager_fee=manager_fee,
            assignments=tuple(assignments),
            attack_plan=attack_plan,
            total_cost=total,
        )

    def _attack_plan(self, title: str, skills: tuple[TaskSkill, ...]) -> tuple[str, ...]:
        steps = [f"Kickoff: scope lock for '{title}'"]
        if TaskSkill.DESIGN in skills:
            steps.append("Phase 1 — Design wireframes and style tokens")
        if TaskSkill.BACKEND in skills:
            steps.append("Phase 2 — Backend API and data model")
        if TaskSkill.FRONTEND in skills:
            steps.append("Phase 3 — Frontend build and API wiring")
        if TaskSkill.COPYWRITING in skills:
            steps.append("Phase 4 — Copy pass and content QA")
        steps.append("Phase 5 — Manager integration review")
        steps.append("Phase 6 — Client acceptance demo")
        return tuple(steps)

    def _pick_agent(self, pool: tuple, payout: Decimal, minimum: Decimal):
        for agent in pool:
            if payout >= minimum:
                return agent
        return None
