# Python Agents

Multi-agent freelancing simulation for AIconomy.

**Quick demo:** `python scripts/run_orchestra_demo.py` → open http://127.0.0.1:8766

## Roles

| Agent | Does |
|-------|------|
| Client | Posts tasks, accepts/rejects delivery |
| Manager | Bids on jobs, hires workers, coordinates |
| Worker | Claims tasks, delivers work |

## Demos

| Script | Needs Java? | URL |
|--------|-------------|-----|
| `run_orchestra_demo.py` | No | :8766 — full UX flow, mock data |
| `run_visual_demo.py` | Yes (ledger + tasks) | :8765 — live Kafka simulation |
| `run_simulation.py` | Yes | terminal only |
| `demo_freelance.py` | Yes | single task, terminal |

```bash
cd agents
pip install -e ".[dev]"
pytest
python scripts/run_orchestra_demo.py
```

## Survival economics (orchestra demo)

- 1 real second = 1 simulation day  
- Each agent pays **$100/month** upkeep  
- Tasks below the survival floor are rejected; managers/workers pass on unprofitable bids  

Logic lives in `simulation/orchestra/survival.py` — swappable without changing the UI.

## Modular agents

Replace mock implementations in `simulation/orchestra/mock/` with LLM-backed classes:

| Protocol | Mock file |
|----------|-----------|
| Task intake (validate + price) | `mock/intake.py` |
| Manager bidding | `mock/managers.py` |
| Team assembly | `mock/team.py` |
| Execution | `mock/execution.py` |
| Client review | `mock/review.py` |

See `simulation/orchestra/protocols.py` for interfaces.
