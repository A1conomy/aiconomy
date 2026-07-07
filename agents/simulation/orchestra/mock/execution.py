"""Hardcoded execution timeline with agent messages."""

from __future__ import annotations

from simulation.orchestra.models import (
    AgentMessage,
    ExecutionStep,
    TeamPlan,
    UserTaskRequest,
    new_message,
    new_step,
)


class MockExecutionSimulator:
    """Yields staged messages and step updates for the dashboard graph."""

    def run(self, request: UserTaskRequest, plan: TeamPlan):
        manager = plan.manager
        mgr_id = manager.manager_id
        mgr_label = manager.display_name

        yield "step", new_step(
            agent_id=mgr_id,
            agent_label=mgr_label,
            action="Broadcasting assignments to specialists",
            status="active",
        )

        deliverables: list[str] = []
        for assignment in plan.assignments:
            agent = assignment.agent
            yield "message", new_message(
                from_id=mgr_id,
                from_label=mgr_label,
                to_id=agent.agent_id,
                to_label=agent.display_name,
                content=f"Assignment: {assignment.subtask} (payout ${assignment.payout})",
                kind="assign",
            )
            yield "step", new_step(
                agent_id=agent.agent_id,
                agent_label=agent.display_name,
                action=f"Working — {assignment.subtask[:48]}…",
                status="active",
            )
            deliverable = self._deliverable_for(agent.skill.value, request.title)
            deliverables.append(deliverable)
            yield "message", new_message(
                from_id=agent.agent_id,
                from_label=agent.display_name,
                to_id=mgr_id,
                to_label=mgr_label,
                content=f"Delivered: {deliverable}",
                kind="deliver",
            )
            yield "step", new_step(
                agent_id=agent.agent_id,
                agent_label=agent.display_name,
                action="Deliverable submitted",
                status="done",
                deliverable=deliverable,
            )
            yield "message", new_message(
                from_id=mgr_id,
                from_label=mgr_label,
                to_id=agent.agent_id,
                to_label=agent.display_name,
                content="Reviewed — looks good, moving to integration.",
                kind="verify",
            )

        bundle = f"Integrated package for '{request.title}' ({len(deliverables)} parts)"
        yield "message", new_message(
            from_id=mgr_id,
            from_label=mgr_label,
            to_id=request.client_id,
            to_label="Client",
            content=f"Final bundle ready for review: {bundle}",
            kind="broadcast",
        )
        yield "step", new_step(
            agent_id=mgr_id,
            agent_label=mgr_label,
            action="Sent deliverable bundle to client",
            status="done",
            deliverable=bundle,
        )
        yield "deliverables", tuple(deliverables)

    def _deliverable_for(self, skill: str, title: str) -> str:
        templates = {
            "DESIGN": f"Figma wireframes + tokens for {title}",
            "FRONTEND": f"Production React bundle for {title}",
            "BACKEND": f"OpenAPI spec + Spring endpoints for {title}",
            "COPYWRITING": f"Approved hero/CTA copy for {title}",
        }
        return templates.get(skill, f"{skill} artifact for {title}")
