#!/usr/bin/env python3
"""Run the worker agent Kafka consumer loop."""

import logging
import os
import sys
from uuid import UUID

from common.events import TaskSkill
from worker_agent.consumer import run_tasks_posted_loop

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s — %(message)s")


def main() -> None:
    worker_agent_id = os.getenv("WORKER_AGENT_ID", "worker-demo")
    worker_account_id = UUID(os.environ["WORKER_ACCOUNT_ID"])
    worker_skill = TaskSkill(os.getenv("WORKER_SKILL", "FRONTEND"))
    max_messages_raw = os.getenv("WORKER_MAX_MESSAGES")
    max_messages = int(max_messages_raw) if max_messages_raw else None

    logging.info(
        "Starting worker consumer: agent=%s skill=%s max_messages=%s",
        worker_agent_id,
        worker_skill.value,
        max_messages,
    )

    claims = run_tasks_posted_loop(
        worker_agent_id=worker_agent_id,
        worker_account_id=worker_account_id,
        worker_skill=worker_skill,
        max_messages=max_messages,
    )
    logging.info("Worker consumer finished — claims=%s", claims)


if __name__ == "__main__":
    try:
        main()
    except KeyError as ex:
        print(f"Missing required environment variable: {ex.args[0]}", file=sys.stderr)
        sys.exit(1)
