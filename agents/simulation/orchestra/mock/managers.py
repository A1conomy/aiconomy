"""Mock manager bidding pool — one real thinker, rest decorative."""

from __future__ import annotations

from decimal import Decimal

from simulation.orchestra.mock.registry import MANAGER_POOL
from simulation.orchestra.models import ManagerBid, UserTaskRequest, ValidationResult
from simulation.orchestra.survival import minimum_acceptable_payout, minimum_task_budget


class MockManagerPool:
    """Returns deterministic bids; lowest fee among accepting managers wins."""

    def collect_bids(
        self, request: UserTaskRequest, validation: ValidationResult
    ) -> tuple[ManagerBid, ...]:
        if not validation.accepted or validation.estimated_cost is None:
            return ()

        total = validation.estimated_cost
        duration_days = validation.estimated_duration_days or 7
        team_size = len(validation.required_skills) + 1
        team_floor = validation.survival_floor or minimum_task_budget(duration_days, team_size)
        min_manager_fee = minimum_acceptable_payout(duration_days)

        bids: list[ManagerBid] = []
        for manager in MANAGER_POOL:
            fee_amount = (total * manager.fee_percent / Decimal("100")).quantize(Decimal("0.01"))
            pitch = self._pitch_for(manager.manager_id, request.title)

            if total < team_floor:
                bids.append(
                    ManagerBid(
                        manager=manager,
                        fee_amount=fee_amount,
                        pitch=pitch,
                        total_quote=total,
                        accepted=False,
                        decline_reason=(
                            f"Total ${total} below team survival floor ${team_floor} "
                            f"({duration_days} sim-days)."
                        ),
                    )
                )
                continue

            if fee_amount < min_manager_fee:
                bids.append(
                    ManagerBid(
                        manager=manager,
                        fee_amount=fee_amount,
                        pitch=pitch,
                        total_quote=total,
                        accepted=False,
                        decline_reason=(
                            f"Manager fee ${fee_amount} below survival minimum "
                            f"${min_manager_fee} for {duration_days} sim-days."
                        ),
                    )
                )
                continue

            bids.append(
                ManagerBid(
                    manager=manager,
                    fee_amount=fee_amount,
                    pitch=pitch,
                    total_quote=total,
                )
            )

        return tuple(sorted(bids, key=lambda bid: (not bid.accepted, bid.manager.fee_percent)))

    def select_winner(self, bids: tuple[ManagerBid, ...]) -> ManagerBid:
        accepting = tuple(bid for bid in bids if bid.accepted)
        if not accepting:
            raise ValueError("No manager accepted the task — budget below agent survival floor")
        return min(accepting, key=lambda bid: bid.manager.fee_percent)

    def _pitch_for(self, manager_id: str, title: str) -> str:
        pitches = {
            "mgr-alpha": f"We will staff senior designers for '{title}' with weekly checkpoints.",
            "mgr-beta": f"Sprint-based delivery for '{title}' — demo every 48h.",
            "mgr-gamma": f"Lean squad for '{title}': one specialist per skill, transparent cost split.",
            "mgr-delta": f"Design-first approach to '{title}' with integrated QA.",
            "mgr-epsilon": f"Enterprise PMO wrapper around '{title}' with compliance docs.",
        }
        return pitches.get(manager_id, f"Ready to deliver '{title}'.")
