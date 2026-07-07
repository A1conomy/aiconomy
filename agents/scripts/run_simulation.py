#!/usr/bin/env python3
"""Run a full mock freelancing simulation via Kafka (no LLM)."""

import logging
import sys

from simulation.runner import run_mock_simulation

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s — %(message)s")


def main() -> None:
    logging.info("Starting mock simulation — posting %s projects", 3)
    result = run_mock_simulation()

    print("")
    print("=== Simulation summary ===")
    print(f"Tasks posted:        {result.posted_tasks}")
    print(f"Manager negotiations:{result.negotiations}")
    print(f"Worker completions:  {result.worker_completions}")
    print(f"Client reviews:      {result.client_reviews}")
    print(f"Client balance:      {result.client_balance}")
    for agent_id, balance in result.worker_balances.items():
        print(f"  {agent_id}: {balance}")

    if result.client_reviews < result.posted_tasks:
        print("Warning: not all tasks were accepted by the client.", file=sys.stderr)
        sys.exit(1)

    print("Simulation complete.")


if __name__ == "__main__":
    main()
