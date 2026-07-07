#!/usr/bin/env python3
"""Run mock simulation with a live web dashboard (no LLM)."""

from __future__ import annotations

import argparse
import logging
import sys
import time
from uuid import uuid4

from simulation.dashboard import start_dashboard_server
from simulation.runner import run_mock_simulation
from simulation.watch_state import SimulationPhase, SimulationWatchState

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s — %(message)s")


def main() -> None:
    parser = argparse.ArgumentParser(description="AIconomy visual mock demo")
    parser.add_argument("--port", type=int, default=8765, help="Dashboard port (default: 8765)")
    parser.add_argument("--host", default="127.0.0.1", help="Dashboard host (default: 127.0.0.1)")
    parser.add_argument(
        "--keep-open",
        type=int,
        default=30,
        metavar="SECONDS",
        help="Keep dashboard alive after simulation (default: 30, 0 = exit immediately)",
    )
    args = parser.parse_args()

    watch_state = SimulationWatchState(run_id=uuid4().hex[:8])
    server = start_dashboard_server(watch_state, host=args.host, port=args.port)
    url = f"http://{args.host}:{args.port}"

    print("")
    print("=== AIconomy visual demo ===")
    print(f"Dashboard: {url}")
    print("Requires docker-compose + ledger (:8081) + tasks (:8082)")
    print("")

    try:
        result = run_mock_simulation(watch_state=watch_state)
    except Exception as exc:
        watch_state.fail(str(exc))
        logging.exception("Simulation failed")
        sys.exit(1)

    print("")
    print("=== Simulation summary ===")
    print(f"Tasks posted:        {result.posted_tasks}")
    print(f"Manager negotiations:{result.negotiations}")
    print(f"Worker completions:  {result.worker_completions}")
    print(f"Client reviews:      {result.client_reviews}")
    print(f"Client balance:      {result.client_balance}")
    for agent_id, balance in result.worker_balances.items():
        print(f"  {agent_id}: {balance}")

    if watch_state.phase == SimulationPhase.FAILED:
        print("Simulation did not complete successfully.", file=sys.stderr)
        sys.exit(1)

    if args.keep_open > 0:
        print(f"\nDashboard stays open for {args.keep_open}s — refresh {url}")
        time.sleep(args.keep_open)

    server.shutdown()
    print("Done.")


if __name__ == "__main__":
    main()
