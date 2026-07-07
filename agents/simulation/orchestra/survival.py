"""Agent survival economics — 1 real second = 1 sim day, $100/month upkeep."""

from __future__ import annotations

from dataclasses import dataclass, field
from decimal import Decimal

MONTHLY_MAINTENANCE = Decimal("100.00")
DAYS_PER_MONTH = 30
SECONDS_PER_SIM_DAY = 1
MIN_PROFIT_MARGIN = Decimal("1.15")
DEFAULT_STARTING_BALANCE = Decimal("250.00")


def daily_maintenance() -> Decimal:
    return (MONTHLY_MAINTENANCE / DAYS_PER_MONTH).quantize(Decimal("0.01"))


def maintenance_for_days(days: int) -> Decimal:
    if days <= 0:
        return Decimal("0.00")
    return (daily_maintenance() * days).quantize(Decimal("0.01"))


def minimum_acceptable_payout(duration_days: int) -> Decimal:
    """Lowest payout an agent accepts — upkeep plus margin."""
    return (maintenance_for_days(duration_days) * MIN_PROFIT_MARGIN).quantize(Decimal("0.01"))


def minimum_task_budget(duration_days: int, team_size: int) -> Decimal:
    """Break-even budget for manager + workers over the job duration."""
    if team_size <= 0 or duration_days <= 0:
        return Decimal("0.00")
    return (minimum_acceptable_payout(duration_days) * team_size).quantize(Decimal("0.01"))


def estimate_task_duration_days(*, skill_count: int, description_length: int) -> int:
    """Heuristic job length used for survival checks."""
    base = max(5, skill_count * 5)
    complexity = min(description_length // 40, 12)
    return base + complexity


@dataclass
class SimulationClock:
    """Tracks elapsed simulation days (1 second of wall time = 1 day)."""

    elapsed_days: int = 0

    def advance_seconds(self, seconds: float) -> int:
        days = int(seconds // SECONDS_PER_SIM_DAY) if seconds >= SECONDS_PER_SIM_DAY else (
            1 if seconds > 0 else 0
        )
        self.elapsed_days += days
        return days


@dataclass
class AgentWallet:
    agent_id: str
    display_name: str
    role: str
    balance: Decimal
    total_maintenance_paid: Decimal = field(default_factory=lambda: Decimal("0.00"))

    @property
    def is_solvent(self) -> bool:
        return self.balance > Decimal("0.00")

    def apply_maintenance(self, days: int) -> Decimal:
        charge = maintenance_for_days(days)
        self.balance = (self.balance - charge).quantize(Decimal("0.01"))
        self.total_maintenance_paid = (self.total_maintenance_paid + charge).quantize(Decimal("0.01"))
        return charge

    def credit(self, amount: Decimal) -> None:
        self.balance = (self.balance + amount).quantize(Decimal("0.01"))


@dataclass
class AgentEconomy:
    """In-memory wallets for managers and specialists."""

    wallets: dict[str, AgentWallet] = field(default_factory=dict)

    @classmethod
    def bootstrap(
        cls,
        *,
        manager_profiles: tuple,
        specialist_pool: dict,
        starting_balance: Decimal = DEFAULT_STARTING_BALANCE,
    ) -> AgentEconomy:
        economy = cls()
        for manager in manager_profiles:
            economy.register(
                manager.manager_id,
                manager.display_name,
                "MANAGER",
                starting_balance,
            )
        seen: set[str] = set()
        for agents in specialist_pool.values():
            for agent in agents:
                if agent.agent_id in seen:
                    continue
                seen.add(agent.agent_id)
                economy.register(agent.agent_id, agent.display_name, "WORKER", starting_balance)
        return economy

    def register(self, agent_id: str, display_name: str, role: str, balance: Decimal) -> None:
        self.wallets[agent_id] = AgentWallet(
            agent_id=agent_id,
            display_name=display_name,
            role=role,
            balance=balance,
        )

    def apply_maintenance_all(self, days: int) -> None:
        for wallet in self.wallets.values():
            wallet.apply_maintenance(days)

    def credit(self, agent_id: str, amount: Decimal) -> None:
        wallet = self.wallets.get(agent_id)
        if wallet is not None:
            wallet.credit(amount)

    def solvent_agents(self) -> list[AgentWallet]:
        return [wallet for wallet in self.wallets.values() if wallet.is_solvent]

    def snapshot(self) -> list[dict[str, object]]:
        return [
            {
                "agentId": wallet.agent_id,
                "displayName": wallet.display_name,
                "role": wallet.role,
                "balance": str(wallet.balance),
                "solvent": wallet.is_solvent,
                "maintenancePaid": str(wallet.total_maintenance_paid),
            }
            for wallet in sorted(self.wallets.values(), key=lambda w: (w.role, w.display_name))
        ]
