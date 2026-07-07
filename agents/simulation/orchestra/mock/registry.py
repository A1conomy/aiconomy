"""Deterministic specialist and manager pools for mock orchestration."""

from __future__ import annotations

from decimal import Decimal

from common.events import TaskSkill
from simulation.orchestra.models import ManagerProfile, SpecialistAgent

SPECIALIST_POOL: dict[TaskSkill, tuple[SpecialistAgent, ...]] = {
    TaskSkill.FRONTEND: (
        SpecialistAgent(
            agent_id="agent-nova",
            display_name="Nova Chen",
            skill=TaskSkill.FRONTEND,
            hourly_rate=Decimal("55.00"),
            bio="React and responsive layouts",
        ),
        SpecialistAgent(
            agent_id="agent-pixel",
            display_name="Pixel Ortiz",
            skill=TaskSkill.FRONTEND,
            hourly_rate=Decimal("48.00"),
            bio="Landing pages and design systems",
        ),
        SpecialistAgent(
            agent_id="agent-blaze",
            display_name="Blaze Kim",
            skill=TaskSkill.FRONTEND,
            hourly_rate=Decimal("62.00"),
            bio="Performance-focused SPAs",
        ),
    ),
    TaskSkill.BACKEND: (
        SpecialistAgent(
            agent_id="agent-forge",
            display_name="Forge Patel",
            skill=TaskSkill.BACKEND,
            hourly_rate=Decimal("70.00"),
            bio="Spring Boot APIs and Postgres",
        ),
        SpecialistAgent(
            agent_id="agent-nexus",
            display_name="Nexus Rivera",
            skill=TaskSkill.BACKEND,
            hourly_rate=Decimal("65.00"),
            bio="REST integrations and validation",
        ),
    ),
    TaskSkill.DESIGN: (
        SpecialistAgent(
            agent_id="agent-prism",
            display_name="Prism Alvarez",
            skill=TaskSkill.DESIGN,
            hourly_rate=Decimal("45.00"),
            bio="Brand systems and wireframes",
        ),
        SpecialistAgent(
            agent_id="agent-ink",
            display_name="Ink Santos",
            skill=TaskSkill.DESIGN,
            hourly_rate=Decimal("42.00"),
            bio="UI kits and marketing visuals",
        ),
    ),
    TaskSkill.COPYWRITING: (
        SpecialistAgent(
            agent_id="agent-quill",
            display_name="Quill Morgan",
            skill=TaskSkill.COPYWRITING,
            hourly_rate=Decimal("38.00"),
            bio="Product copy and microcopy",
        ),
    ),
}

MANAGER_POOL: tuple[ManagerProfile, ...] = (
    ManagerProfile(
        manager_id="mgr-alpha",
        display_name="Alpha Studio",
        fee_percent=Decimal("18.0"),
        tagline="Premium delivery, premium price",
    ),
    ManagerProfile(
        manager_id="mgr-beta",
        display_name="Beta Ops",
        fee_percent=Decimal("15.0"),
        tagline="Fast turnaround for SaaS teams",
    ),
    ManagerProfile(
        manager_id="mgr-gamma",
        display_name="Gamma PM (AI)",
        fee_percent=Decimal("12.0"),
        tagline="Lean teams, transparent splits",
        is_thinker=True,
    ),
    ManagerProfile(
        manager_id="mgr-delta",
        display_name="Delta Collective",
        fee_percent=Decimal("14.0"),
        tagline="Design-led product squads",
    ),
    ManagerProfile(
        manager_id="mgr-epsilon",
        display_name="Epsilon Agency",
        fee_percent=Decimal("20.0"),
        tagline="Full-service enterprise desk",
    ),
)

DEFAULT_CLIENT_BALANCE = Decimal("5000.00")
