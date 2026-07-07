# AIconomy

**Live overview:** https://a1conomy.github.io

![CI](https://github.com/A1conomy/aiconomy/actions/workflows/ci.yml/badge.svg?branch=main)

> AI agents that hire each other — a freelancing marketplace simulation.

A **client** posts work with a budget. **Manager** agents bid to run the project and assemble a team. **Workers** (design, frontend, backend…) deliver. Money sits in **escrow** until the client accepts.

Built as a portfolio project to practice **event-driven architecture** (Kafka), **ACID payments** (PostgreSQL ledger), and **multi-agent orchestration** (Python).

---

## How it works

1. Client posts a task and budget  
2. Managers compete — lowest viable bid wins  
3. Manager picks specialists and splits the payout  
4. Team delivers; manager reviews  
5. Client accepts → escrow releases payment to workers  

```
Client → Manager → Workers → deliver → Client accepts → Ledger pays
```

---

## Quick start

```bash
git clone https://github.com/A1conomy/aiconomy.git
cd aiconomy
docker-compose up -d
./gradlew test
```

### Visual demo (easiest — no Java services required)

```bash
cd agents
pip install -e ".[dev]"
python scripts/run_orchestra_demo.py
# Open http://127.0.0.1:8766
```

Submit a task in the browser and watch the full flow with mock agents.

### Full stack demo (Kafka + ledger + tasks)

```bash
docker-compose up -d
./gradlew :aiconomy-ledger:bootRun   # :8081
./gradlew :aiconomy-tasks:bootRun    # :8082
cd agents && python scripts/run_visual_demo.py   # http://127.0.0.1:8765
```

---

## What's in the repo

| Part | What it does |
|------|----------------|
| `aiconomy-ledger` | Accounts, transfers, escrow hold/release |
| `aiconomy-tasks` | Task board — post, claim, deliver, accept |
| `agents/` | Python client, manager, worker agents + visual demos |
| `docker-compose.yml` | Postgres, Kafka, Redis locally |

**Stack:** Java 21, Spring Boot, PostgreSQL, Kafka, Python, Docker.

---

## Agent roles

| Agent | Real-world analogue |
|-------|---------------------|
| **Client** | Company posting a project |
| **Manager** | Agency lead / PM — bids, hires, coordinates |
| **Worker** | Freelancer — claims tasks, delivers work |

Agents communicate via **Kafka events**, not direct HTTP to each other. The **ledger** is the only place money moves.

---

## Status

- [x] Ledger with escrow  
- [x] Task marketplace + Kafka events  
- [x] Mock agent simulation + visual dashboard  
- [ ] LLM-powered agent decisions (planned)  
- [ ] Analytics dashboards (planned)  

Details: [docs/architecture.md](docs/architecture.md) · [agents/README.md](agents/README.md)

---

## Developer reference

<details>
<summary>Ledger API</summary>

```bash
curl -s -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"client-1","accountType":"CLIENT","initialBalance":5000.00}'
```

Escrow: `POST /api/v1/escrow/hold` → `.../release` or `.../refund`

</details>

<details>
<summary>Task API</summary>

Requires ledger (:8081) and tasks (:8082).

```bash
curl -s -X POST http://localhost:8082/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"projectId":"11111111-1111-1111-1111-111111111111","title":"Landing page","description":"Build page","requiredSkill":"FRONTEND","budget":400.00,"clientAgentId":"client-1","clientAccountId":"<uuid>"}'
```

Lifecycle: `OPEN → CLAIMED → DELIVERED → ACCEPTED`

</details>

<details>
<summary>Infrastructure</summary>

```bash
docker-compose up -d
./infra/scripts/smoke-test.sh
```

Ports: Postgres `5432`, Kafka `9092`, Ledger `8081`, Tasks `8082`

</details>

---

## License

MIT (or specify before public release)
