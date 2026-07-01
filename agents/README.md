# Python Agents

Multi-agent freelancing simulation for AIconomy. Agents communicate via Kafka only; the Spring ledger handles payments and (future) escrow.

## Agent roles

| Package | Role |
|---------|------|
| `client_agent/` | Posts projects, accepts or rejects deliveries, owns budget |
| `manager_agent/` | Negotiates with clients, hires workers, orchestrates delivery |
| `worker_agent/` | Specialized freelancer — claims tasks, delivers, subcontracts |
| `common/` | Config, ledger client, LLM factory, Kafka topic constants |

## Status

Baseline only — role packages are placeholders. Implementation starts with task service + escrow in Java, then agent loops.

```bash
cd agents
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pytest
```
