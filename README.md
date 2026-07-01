# AIconomy

![CI](https://github.com/A1conomy/aiconomy/actions/workflows/ci.yml/badge.svg?branch=main)

> Event-Driven Multi-Agent Freelancing Economy

AIconomy simulates a **services marketplace** where AI agents negotiate, delegate, and get paid for work. Clients post projects; **manager agents** negotiate price and orchestrate delivery; **worker agents** claim specialized tasks and collaborate via subcontracting. A **Core Banking Ledger** (Spring Boot) provides ACID payments and (planned) escrow.

Built as a **Gradle multi-module / microservices-ready** platform for learning and demonstrating enterprise + agentic system design.

---

## Architecture

```mermaid
flowchart TB
    subgraph agents [Python Agents]
        Client[ClientAgent]
        Manager[ManagerAgent]
        Worker[WorkerAgent]
    end

    subgraph java [Java Services]
        Tasks[aiconomy-tasks planned]
        Ledger[aiconomy-ledger :8081]
        Analytics[aiconomy-analytics planned]
        Common[aiconomy-common]
    end

    subgraph infra [Infrastructure]
        Kafka[Apache Kafka]
        Postgres[(PostgreSQL)]
    end

    Client -->|tasks.posted| Kafka
    Manager -->|negotiate + hire| Kafka
    Worker -->|tasks.claimed / delivered| Kafka
    Kafka --> Tasks
    Tasks --> Ledger
    Ledger --> Postgres
    Analytics --> Postgres
    Analytics --> Kafka
```

| Bounded context | Responsibility |
|-----------------|----------------|
| **Ledger** | ACID accounts, transfers, escrow hold/release (planned) |
| **Tasks** | Task board — post, claim, deliver, accept/reject (planned) |
| **Analytics** | Macro metrics — task volume, avg rates, utilization (planned) |
| **Agents** | Client, manager, worker roles via Kafka (no direct agent-to-agent HTTP) |

See [docs/architecture.md](docs/architecture.md) for ADRs.

### Agent roles

| Agent | Real-world analogue | Responsibilities |
|-------|---------------------|------------------|
| **ClientAgent** | Company / project owner | Posts projects, accepts or rejects deliveries, pays from budget |
| **ManagerAgent** | Agency lead / PM | Negotiates price with client, hires workers, monitors progress |
| **WorkerAgent** | Freelancer | Claims tasks, delivers work, negotiates subcontract pay |

---

## Tech Stack

| Layer | Technology | Role |
|-------|------------|------|
| Core | Spring Boot 4.x, Java 21 | Ledger + task service |
| Messaging | Apache Kafka (KRaft) | Task lifecycle, payments, simulation clock |
| Ledger DB | PostgreSQL | ACID source of truth |
| AI agents | Python, LangGraph (planned) | Negotiation, orchestration, delivery |
| LLM (dev) | Ollama | Unlimited local iteration |
| LLM (prod) | Gemini API | Demo-quality decisions |
| Observability | Micrometer, Prometheus, Grafana (planned) | Tech + economy dashboards |
| Runtime | Docker Compose | Local full stack |

---

## Prerequisites

- **Java 21** (JDK)
- **Docker** & Docker Compose
- **Python 3.11+** (for agents)
- **Ollama** (optional — [ollama.ai](https://ollama.ai))
- **Git**

---

## Quick Start

```bash
git clone https://github.com/A1conomy/aiconomy.git
cd aiconomy

cp .env.example .env

docker-compose up -d
./infra/scripts/smoke-test.sh

./gradlew test

./gradlew :aiconomy-ledger:bootRun
curl http://localhost:8081/actuator/health
```

> **New to the stack?** Read [docs/infrastructure.md](docs/infrastructure.md).

---

## Infrastructure

| Service | Host port | Container | Purpose |
|---------|-----------|-----------|---------|
| PostgreSQL 16 | `5432` | `aiconomy-postgres` | Ledger database |
| Redis 7 | `6379` | `aiconomy-redis` | Reserved for future hot state |
| Kafka 3.8 (KRaft) | `9092` | `aiconomy-kafka` | Event backbone |

**Kafka topics** (auto-created): `tasks.posted`, `tasks.claimed`, `tasks.delivered`, `tasks.accepted`, `tasks.rejected`, `payments.proposed`, `payments.accepted`, `ledger.commands`, `ledger.events`, `macro.snapshots`, `simulation.tick`

```bash
docker-compose up -d
docker-compose down
docker-compose down -v        # wipe data
./infra/scripts/smoke-test.sh
```

---

## Modules

| Module | Port | Status | Description |
|--------|------|--------|-------------|
| `aiconomy-common` | — | Active | Kafka topic constants |
| `aiconomy-ledger` | 8081 | Active | Core banking — accounts, ACID transfers |
| `aiconomy-tasks` | 8082 | Planned | Task board + lifecycle |
| `aiconomy-analytics` | 8083 | Planned | Macro metrics |
| `agents/` | — | Baseline | Python agent packages (placeholders) |

```bash
./gradlew :aiconomy-common:test
./gradlew :aiconomy-ledger:bootRun
./gradlew :aiconomy-ledger:test
```

---

## Ledger API

With `docker-compose up` and `./gradlew :aiconomy-ledger:bootRun`:

```bash
curl -s -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"client-1","accountType":"CLIENT","initialBalance":5000.00}'

curl -s -X POST http://localhost:8081/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"<source-uuid>","toAccountId":"<dest-uuid>","amount":200.00}'

curl -s http://localhost:8081/api/v1/accounts/<account-uuid>
```

---

## Python Agents (baseline)

Role packages exist as placeholders; Kafka loops and LangGraph graphs are not implemented yet.

```bash
cd agents
python -m venv .venv && source .venv/bin/activate
pip install -e ".[dev]"
pytest
```

| Package | Role |
|---------|------|
| `client_agent/` | Post projects, accept/reject deliveries |
| `manager_agent/` | Negotiate with clients, hire and monitor workers |
| `worker_agent/` | Claim tasks, deliver, subcontract |
| `common/llm/` | `create_provider()` — mock / Ollama / Gemini |
| `common/clients/ledger.py` | Account bootstrap via REST |

See [agents/README.md](agents/README.md).

---

## Environment

Copy [.env.example](.env.example) to `.env`. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_PROVIDER` | `ollama` | `mock`, `ollama`, or `gemini` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `POSTGRES_*` | see `.env.example` | Ledger database |
| `LEDGER_BASE_URL` | `http://localhost:8081` | Ledger REST API |

---

## Testing

```bash
./gradlew test                              # Java — runs in GitHub Actions
./infra/scripts/smoke-test.sh               # Docker infra
./infra/scripts/e2e-ledger.sh               # ledger must be running
cd agents && pytest                         # Python unit tests
```

---

## Roadmap

- [x] **M0a** — GitHub repo, Cursor rules, README
- [x] **M0b** — Docker Compose + smoke test
- [x] **M0c** — Gradle multi-project skeleton
- [x] **M1** — Ledger microservice (ACID transfers, REST, concurrency test)
- [x] **M5a** — CI (GitHub Actions)
- [ ] **M3** — Task marketplace + agents *(pivot — baseline committed)*
  - [ ] M3a — Task domain + `aiconomy-tasks` service
  - [ ] M3b — Ledger escrow (hold / release)
  - [ ] M3c — ClientAgent + ManagerAgent + WorkerAgent loops
  - [ ] M3d — Payment negotiation + LangGraph + LLM
- [ ] **M4** — Analytics + observability
- [ ] **M5b** — CV polish, E2E in CI

**Retired:** WIDGET order-book market (M2/M2b) — replaced by task marketplace pivot (ADR-005).

---

## Contributing

Portfolio / learning project. [Conventional Commits](https://www.conventionalcommits.org/). See `.cursor/rules/`.

---

## License

MIT (or specify before public release)
