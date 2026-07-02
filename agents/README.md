# Python Agents

Multi-agent freelancing simulation for AIconomy.

## Agent roles

| Package | Role |
|---------|------|
| `client_agent/` | Posts tasks, accepts/rejects deliveries |
| `manager_agent/` | Negotiates manager fee via Kafka (`payments.proposed`) |
| `worker_agent/` | Claims matching tasks, delivers work |
| `common/` | Config, ledger/tasks HTTP clients, Kafka producer, events |

## Run tests

```bash
cd agents
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pytest
```

## Live demo

Requires `docker-compose up`, ledger on `:8081`, and tasks on `:8082`:

```bash
python scripts/demo_freelance.py
```

Flow: create accounts → client posts task → manager negotiates fee on Kafka → worker claims (escrow hold) → worker delivers → client accepts (escrow release).
