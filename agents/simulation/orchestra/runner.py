"""Runs the staged orchestration flow with pauses for the visual dashboard."""

from __future__ import annotations

import logging
import time
from decimal import Decimal

from simulation.orchestra.mock import (
    MockClientReviewAgent,
    MockExecutionSimulator,
    MockManagerPool,
    MockTaskIntakeAgent,
    MockTeamAssembler,
)
from simulation.orchestra.mock.registry import DEFAULT_CLIENT_BALANCE, MANAGER_POOL, SPECIALIST_POOL
from simulation.orchestra.models import (
    AgentMessage,
    ExecutionStep,
    MockBalances,
    OrchestrationPhase,
    OrchestrationResult,
    UserTaskRequest,
)
from simulation.orchestra.protocols import (
    ClientReviewAgent,
    ExecutionSimulator,
    ManagerPool,
    TaskIntakeAgent,
    TeamAssembler,
)
from simulation.orchestra.state import OrchestrationWatchState
from simulation.orchestra.survival import AgentEconomy, SimulationClock

logger = logging.getLogger(__name__)


def run_orchestration(
    request: UserTaskRequest,
    *,
    watch_state: OrchestrationWatchState | None = None,
    intake: TaskIntakeAgent | None = None,
    managers: ManagerPool | None = None,
    assembler: TeamAssembler | None = None,
    executor: ExecutionSimulator | None = None,
    reviewer: ClientReviewAgent | None = None,
    step_delay_seconds: float = 2.0,
    bid_delay_seconds: float = 1.2,
    message_delay_seconds: float = 0.8,
    initial_client_balance: Decimal = DEFAULT_CLIENT_BALANCE,
) -> OrchestrationResult:
    """Execute the full user-facing demo flow with optional live state updates."""
    intake_agent = intake or MockTaskIntakeAgent()
    manager_pool = managers or MockManagerPool()
    team_assembler = assembler or MockTeamAssembler()
    execution = executor or MockExecutionSimulator()
    client_reviewer = reviewer or MockClientReviewAgent()
    clock = SimulationClock()
    economy = AgentEconomy.bootstrap(
        manager_profiles=MANAGER_POOL,
        specialist_pool=SPECIALIST_POOL,
    )

    if watch_state is not None:
        watch_state.reset_for_run(request)
        watch_state.attach_survival(clock, economy)
        watch_state.append_event(f"Task submitted: {request.title}")
        watch_state.append_event("Survival clock started — 1 second = 1 sim-day, $100/month upkeep")

    _pause(step_delay_seconds * 0.5, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.VALIDATING)
    validation = intake_agent.validate(request)
    if watch_state is not None:
        watch_state.set_validation(validation)
        watch_state.append_event(
            "AI intake: accepted" if validation.accepted else f"AI intake rejected — {validation.reason}"
        )

    if not validation.accepted:
        _set_phase(watch_state, OrchestrationPhase.REJECTED)
        if watch_state is not None:
            watch_state.mark_idle()
        return OrchestrationResult(
            request=request,
            validation=validation,
            phase=OrchestrationPhase.REJECTED,
        )

    _pause(step_delay_seconds, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.MANAGER_BIDDING)
    all_bids = manager_pool.collect_bids(request, validation)
    try:
        if isinstance(manager_pool, MockManagerPool):
            winner = manager_pool.select_winner(all_bids)
        else:
            accepting = [bid for bid in all_bids if bid.accepted]
            if not accepting:
                raise ValueError("No manager accepted the task")
            winner = min(accepting, key=lambda bid: bid.fee_amount)
    except ValueError as exc:
        for bid in sorted(all_bids, key=lambda b: b.fee_amount):
            if watch_state is not None:
                watch_state.add_bid(bid)
                if bid.accepted:
                    watch_state.append_event(
                        f"Bid from {bid.manager.display_name}: {bid.manager.fee_percent}% (${bid.fee_amount})"
                    )
                else:
                    watch_state.append_event(
                        f"{bid.manager.display_name} passed — {bid.decline_reason}"
                    )
        _set_phase(watch_state, OrchestrationPhase.REJECTED)
        if watch_state is not None:
            watch_state.append_event(str(exc))
            watch_state.mark_idle()
        return OrchestrationResult(
            request=request,
            validation=validation,
            phase=OrchestrationPhase.REJECTED,
        )

    for bid in sorted(all_bids, key=lambda b: b.fee_amount):
        if watch_state is not None:
            watch_state.add_bid(bid)
            if bid.accepted:
                watch_state.append_event(
                    f"Bid from {bid.manager.display_name}: {bid.manager.fee_percent}% (${bid.fee_amount})"
                )
            else:
                watch_state.append_event(f"{bid.manager.display_name} passed — {bid.decline_reason}")
        _pause(bid_delay_seconds, clock, economy, watch_state)

    if watch_state is not None:
        watch_state.set_winning_bid(winner)
        watch_state.append_event(f"Winner: {winner.manager.display_name} (lowest fee)")

    _pause(step_delay_seconds, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.TEAM_PLANNING)
    try:
        plan = team_assembler.build_plan(request, validation, winner)
    except ValueError as exc:
        _set_phase(watch_state, OrchestrationPhase.REJECTED)
        if watch_state is not None:
            watch_state.append_event(str(exc))
            watch_state.mark_idle()
        return OrchestrationResult(
            request=request,
            validation=validation,
            winning_bid=winner,
            phase=OrchestrationPhase.REJECTED,
        )
    if watch_state is not None:
        watch_state.set_team_plan(plan)
        watch_state.append_event(
            f"Team assembled — {len(plan.assignments)} specialists, total ${plan.total_cost}"
        )

    _pause(step_delay_seconds, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.EXECUTING)
    deliverables: tuple[str, ...] = ()
    for event_type, payload in execution.run(request, plan):
        if event_type == "message":
            message: AgentMessage = payload  # type: ignore[assignment]
            if watch_state is not None:
                watch_state.add_message(message)
                watch_state.append_event(f"{message.from_label} → {message.to_label}")
            _pause(message_delay_seconds, clock, economy, watch_state)
        elif event_type == "step":
            step: ExecutionStep = payload  # type: ignore[assignment]
            if watch_state is not None:
                watch_state.upsert_step(step)
        elif event_type == "deliverables":
            deliverables = payload  # type: ignore[assignment]
            if watch_state is not None:
                watch_state.set_deliverables(deliverables)

    _pause(step_delay_seconds, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.CLIENT_REVIEW)
    review = client_reviewer.review(
        request,
        deliverables,
        str(plan.total_cost),
    )
    if watch_state is not None:
        watch_state.set_client_outcome(accepted=review.accepted, feedback=review.feedback)
        watch_state.append_event("Client accepted — releasing escrow" if review.accepted else "Client rejected")

    balances = _settle_balances(
        initial_client_balance=initial_client_balance,
        plan=plan,
        payment_released=review.accepted,
        economy=economy,
    )
    if watch_state is not None:
        watch_state.set_balances(balances)

    final_phase = OrchestrationPhase.PAID if review.accepted else OrchestrationPhase.PAYMENT_HELD
    _set_phase(watch_state, final_phase)
    _pause(step_delay_seconds * 0.5, clock, economy, watch_state)
    _set_phase(watch_state, OrchestrationPhase.COMPLETE)
    if watch_state is not None:
        watch_state.mark_idle()
        watch_state.append_event(f"Orchestration complete — sim day {clock.elapsed_days}")

    return OrchestrationResult(
        request=request,
        validation=validation,
        winning_bid=winner,
        team_plan=plan,
        client_review=review,
        final_balances=balances,
        phase=OrchestrationPhase.COMPLETE,
    )


def _settle_balances(
    *,
    initial_client_balance: Decimal,
    plan,
    payment_released: bool,
    economy: AgentEconomy,
) -> MockBalances:
    if not payment_released:
        return MockBalances(
            client_balance=initial_client_balance,
            manager_balance=economy.wallets[plan.manager.manager_id].balance,
            worker_balances={
                agent_id: wallet.balance
                for agent_id, wallet in economy.wallets.items()
                if wallet.role == "WORKER"
            },
        )

    economy.credit(plan.manager.manager_id, plan.manager_fee)
    worker_balances: dict[str, Decimal] = {}
    for assignment in plan.assignments:
        economy.credit(assignment.agent.agent_id, assignment.payout)
        worker_balances[assignment.agent.agent_id] = economy.wallets[assignment.agent.agent_id].balance

    total_paid = plan.total_cost
    return MockBalances(
        client_balance=(initial_client_balance - total_paid).quantize(Decimal("0.01")),
        manager_balance=economy.wallets[plan.manager.manager_id].balance,
        worker_balances=worker_balances,
    )


def _set_phase(watch_state: OrchestrationWatchState | None, phase: OrchestrationPhase) -> None:
    if watch_state is not None:
        watch_state.set_phase(phase)
    logger.info("Phase: %s", phase.value)


def _pause(
    seconds: float,
    clock: SimulationClock,
    economy: AgentEconomy,
    watch_state: OrchestrationWatchState | None,
) -> None:
    if seconds <= 0:
        return
    days = clock.advance_seconds(seconds)
    if days > 0:
        economy.apply_maintenance_all(days)
        if watch_state is not None:
            watch_state.refresh_survival()
            insolvent = [w.display_name for w in economy.wallets.values() if not w.is_solvent]
            if insolvent:
                watch_state.append_event(f"Maintenance charged ({days}d) — bankrupt: {', '.join(insolvent)}")
            else:
                watch_state.append_event(f"Maintenance charged — {days} sim-day(s) elapsed (total day {clock.elapsed_days})")
    time.sleep(seconds)
