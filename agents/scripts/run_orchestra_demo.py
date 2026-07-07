#!/usr/bin/env python3
"""Staged orchestration demo — user task, mock AI, bidding, team graph (no Kafka)."""

from __future__ import annotations

import argparse
import logging
import sys
import time
from uuid import uuid4

from simulation.orchestra.dashboard import start_orchestra_server
from simulation.orchestra.models import OrchestrationPhase, UserTaskRequest
from simulation.orchestra.runner import run_orchestration
from simulation.orchestra.state import OrchestrationWatchState

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s — %(message)s")

DEFAULT_TASK = UserTaskRequest(
    title="Product launch landing page",
    description=(
        "Responsive landing page with hero section, signup form, and REST API "
        "integration for user registration (Spring Boot + Postgres)."
    ),
)


def main() -> None:
    parser = argparse.ArgumentParser(description="AIconomy orchestration visual demo")
    parser.add_argument("--port", type=int, default=8766, help="Dashboard port (default: 8766)")
    parser.add_argument("--host", default="127.0.0.1", help="Dashboard host")
    parser.add_argument(
        "--auto-start",
        action="store_true",
        help="Run default task immediately (otherwise submit via browser form)",
    )
    parser.add_argument("--title", help="Task title for --auto-start")
    parser.add_argument("--description", help="Task description for --auto-start")
    parser.add_argument(
        "--step-delay",
        type=float,
        default=2.0,
        help="Seconds between major phases (default: 2.0)",
    )
    parser.add_argument(
        "--keep-open",
        type=int,
        default=120,
        metavar="SECONDS",
        help="Keep dashboard alive after run (default: 120, 0 = until Ctrl+C)",
    )
    args = parser.parse_args()

    watch_state = OrchestrationWatchState(run_id=uuid4().hex[:8])
    server = start_orchestra_server(
        watch_state,
        host=args.host,
        port=args.port,
        step_delay_seconds=args.step_delay,
    )
    url = f"http://{args.host}:{args.port}"

    print("")
    print("=== AIconomy orchestration demo ===")
    print(f"Dashboard: {url}")
    print("No Kafka/ledger required — fully mocked orchestration.")
    print("Submit a task in the browser, or re-run with --auto-start")
    print("")

    if args.auto_start:
        request = UserTaskRequest(
            title=args.title or DEFAULT_TASK.title,
            description=args.description or DEFAULT_TASK.description,
        )
        try:
            result = run_orchestration(
                request,
                watch_state=watch_state,
                step_delay_seconds=args.step_delay,
            )
        except Exception as exc:
            watch_state.fail(str(exc))
            logging.exception("Orchestration failed")
            sys.exit(1)

        print("=== Result ===")
        print(f"Phase: {result.phase.value}")
        if result.validation:
            print(f"Validation: {result.validation.reason}")
        if result.winning_bid:
            print(f"Winning manager: {result.winning_bid.manager.display_name}")
        if result.client_review:
            print(f"Client: {result.client_review.feedback}")

        if result.phase == OrchestrationPhase.REJECTED:
            sys.exit(1)

    if args.keep_open == 0 and not args.auto_start:
        print("Press Ctrl+C to stop.")
        try:
            while True:
                time.sleep(3600)
        except KeyboardInterrupt:
            server.shutdown()
            return

    if args.keep_open > 0:
        print(f"Dashboard open for {args.keep_open}s — {url}")
        time.sleep(args.keep_open)

    server.shutdown()
    print("Done.")


if __name__ == "__main__":
    main()
