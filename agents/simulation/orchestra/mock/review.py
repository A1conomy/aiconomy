"""Mock client acceptance gate."""

from __future__ import annotations

from simulation.orchestra.models import ClientReviewResult, UserTaskRequest


class MockClientReviewAgent:
    """Accepts by default; rejects when description hints intentional failure."""

    def review(
        self,
        request: UserTaskRequest,
        deliverables: tuple[str, ...],
        total_cost: str,
    ) -> ClientReviewResult:
        text = f"{request.title} {request.description}".lower()
        if "reject me" in text or "intentionally bad" in text:
            return ClientReviewResult(
                accepted=False,
                feedback="Deliverable does not meet acceptance criteria — payment held in escrow.",
            )
        if not deliverables:
            return ClientReviewResult(
                accepted=False,
                feedback="No deliverables received.",
            )
        summary = ", ".join(deliverables[:2])
        if len(deliverables) > 2:
            summary += f" (+{len(deliverables) - 2} more)"
        return ClientReviewResult(
            accepted=True,
            feedback=f"Accepted. Released ${total_cost} from escrow. Deliverables: {summary}.",
        )
