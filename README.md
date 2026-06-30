# AIconomy

> Event-Driven Agent-Based Macroeconomic Simulation Platform

AIconomy simulates an autonomous economy where AI agents (consumers, firms, central bank) make financial decisions in a distributed, event-driven system. A **Core Banking Ledger** guarantees ACID fund transfers; an **Open Market Matching Engine** matches buy/sell orders in real time; **Python/LangGraph agents** act autonomously based on macroeconomic state.

Built as a **Gradle multi-module / microservices-ready** platform for learning and demonstrating enterprise system design.

---

## Architecture

```mermaid
flowchart TB
    subgraph agents [Python Agents]
        Consumer[ConsumerAgent]
        Firm[FirmAgent]
        CB[CentralBankAgent]
    end

    subgraph java [Java Services]
        Ledger[aiconomy-ledger]
        Market[aiconomy-market]
        Analytics[aiconomy-analytics]
        Common[aiconomy-common]
    end

    subgraph infra [Infrastructure]
        Kafka[Apache Kafka]
        Postgres[(PostgreSQL)]
        Redis[(Redis)]
    end

    Consumer --> Kafka
    Firm --> Kafka
    CB --> Kafka
    Kafka --> Market
    Kafka --> Ledger
    Market --> Redis
    Ledger --> Postgres
    Analytics --> Postgres
    Analytics --> Kafka
```

| Bounded context | Responsibility |
|-----------------|----------------|
| **Ledger** | Central bank, double-entry bookkeeping, ACID transfers |
| **Market** | Order book, price-time matching, trade events |
| **Analytics** | Macro metrics (GDP, inflation, credit volume) |
| **Agents** | Autonomous AI actors via Kafka (no direct agent-to-agent calls) |

See [docs/architecture.md](docs/architecture.md) for ADRs.

---

## Tech Stack

| Layer | Technology | Role |
|-------|------------|------|
| Core | Spring Boot 4.x, Java 21 | Banking & market services |
| Messaging | Apache Kafka (KRaft) | Event backbone, audit, replay |
| Ledger DB | PostgreSQL | ACID source of truth |
| Order book | Redis | Hot in-memory matching state |
| AI agents | Python, LangGraph | Autonomous market participants |
| LLM (dev) | Ollama | Unlimited local iteration |
| LLM (prod) | Gemini API | Demo-quality decisions |
| Observability | Micrometer, Prometheus, Grafana | Tech + macro dashboards |
| Runtime | Docker Compose | Local full stack |

---

## Prerequisites

- **Java 21** (JDK)
- **Docker** & Docker Compose
- **Python 3.11+** (for agents, Milestone 3)
- **Ollama** (optional, for local LLM — [ollama.ai](https://ollama.ai))
- **Git**

---

## Quick Start

```bash
# Clone
git clone https://github.com/A1conomy/aiconomy.git
cd aiconomy

# Environment (optional — defaults match docker-compose)
cp .env.example .env

# Start infrastructure (Postgres, Kafka, Redis)
docker-compose up -d

# Verify all services healthy
./infra/scripts/smoke-test.sh

# Run Spring tests
./gradlew test

# Run ledger service (requires docker-compose up)
./gradlew :aiconomy-ledger:bootRun

# Run market service (requires docker-compose up)
./gradlew :aiconomy-market:bootRun

# Health check (ledger on port 8081, market on port 8082)
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

> **New to the stack?** Read [docs/infrastructure.md](docs/infrastructure.md) — explains Docker, Kafka, Postgres, Redis in AIconomy context.

---

## Infrastructure (M0b)

| Service | Host port | Container | Purpose |
|---------|-----------|-----------|---------|
| PostgreSQL 16 | `5432` | `aiconomy-postgres` | ACID ledger database |
| Redis 7 | `6379` | `aiconomy-redis` | Order book (M2) |
| Kafka 3.8 (KRaft) | `9092` | `aiconomy-kafka` | Event backbone |

**Kafka topics** (auto-created): `orders.submitted`, `trades.executed`, `ledger.commands`, `ledger.events`, `market.quotes`, `macro.snapshots`, `simulation.tick`

```bash
docker-compose up -d          # start
docker-compose down           # stop
docker-compose down -v        # stop + wipe data
./infra/scripts/smoke-test.sh # health check
```

---

## Modules

| Module | Port | Status | Description |
|--------|------|--------|-------------|
| `aiconomy-common` | — | Active | Shared Kafka topic constants & DTOs |
| `aiconomy-ledger` | 8081 | Active | Core banking ledger (accounts, ACID transfers) |
| `aiconomy-market` | 8082 | Active | Matching engine — Redis order book, REST API, ledger settlement |
| `aiconomy-analytics` | 8083 | Planned | Macro metrics |
| `agents/` | — | Planned | Python LangGraph agents |

```bash
./gradlew :aiconomy-common:test     # common module only
./gradlew :aiconomy-ledger:bootRun # run ledger service
./gradlew :aiconomy-ledger:test     # ledger tests (Testcontainers needs Docker)
./gradlew :aiconomy-market:bootRun # run market service
./gradlew :aiconomy-market:test     # market tests
```

---

## Ledger API (M1)

With `docker-compose up` and `./gradlew :aiconomy-ledger:bootRun`:

```bash
# Create account
curl -s -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"agent-1","accountType":"CONSUMER","initialBalance":1000.00}'

# Transfer (use account IDs from create response)
curl -s -X POST http://localhost:8081/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"<source-uuid>","toAccountId":"<dest-uuid>","amount":50.00}'

# Get balance
curl -s http://localhost:8081/api/v1/accounts/<account-uuid>
```

---

## Market API (M2)

Requires **ledger** (8081) and **market** (8082) running, plus Redis from `docker-compose`:

```bash
# Terminal 1 — infrastructure
docker-compose up -d

# Terminal 2 — ledger
./gradlew :aiconomy-ledger:bootRun

# Terminal 3 — market
./gradlew :aiconomy-market:bootRun
```

### End-to-end trade (curl)

```bash
# 1. Create seller account (needs balance to receive payment later)
curl -s -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"seller-1","accountType":"FIRM","initialBalance":0.00}'

# 2. Create buyer account with funds
curl -s -X POST http://localhost:8081/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{"ownerId":"buyer-1","accountType":"CONSUMER","initialBalance":500.00}'

# Copy account UUIDs from the JSON responses, then:

# 3. Resting sell order — 5 WIDGET @ 10.00 (no trade yet)
curl -s -X POST http://localhost:8082/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<seller-uuid>","symbol":"WIDGET","side":"SELL","price":10.00,"quantity":5.00}'

# 4. Matching buy — 3 WIDGET @ 12.00 → trades 3 @ 10.00, settles 30.00 via ledger
curl -s -X POST http://localhost:8082/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<buyer-uuid>","symbol":"WIDGET","side":"BUY","price":12.00,"quantity":3.00}'

# 5. Top of book (2 WIDGET still offered @ 10.00)
curl -s http://localhost:8082/api/v1/market/WIDGET/top

# 6. Verify balances (buyer -30.00, seller +30.00)
curl -s http://localhost:8081/api/v1/accounts/<buyer-uuid>
curl -s http://localhost:8081/api/v1/accounts/<seller-uuid>
```

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/orders` | POST | Submit limit order; match + settle trades |
| `/api/v1/market/{symbol}/top` | GET | Best bid / best ask snapshot |

Settlement follows **ADR-004**: match in Redis → `POST /transfers` on ledger (`price × quantity`).

---

Copy [.env.example](.env.example) to `.env`. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_PROVIDER` | `ollama` | `ollama` or `gemini` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `POSTGRES_*` | see `.env.example` | Ledger database |
| `REDIS_HOST` | `localhost` | Order book cache |
| `LEDGER_BASE_URL` | `http://localhost:8081` | Ledger URL for market settlement |

---

## Testing

```bash
# Java (all modules)
./gradlew test

# Python agents (Milestone 3)
cd agents && pytest
```

---

## Roadmap

- [x] **M0a** — GitHub repo, Cursor rules, README, `.gitignore`
- [x] **M0b** — Docker Compose (Postgres, Kafka, Redis) + smoke test
- [x] **M0c** — Gradle multi-project skeleton + Spring infra connectivity
- [x] **M1** — Ledger microservice (ACID transfers, REST API, concurrency test)
- [x] **M2** — Market matching engine (Redis order book, REST API, ledger settlement)
- [ ] **M2b** — Kafka pipeline (`orders.submitted` / `trades.executed`) *(follow-up)*
- [ ] **M3** — Python agents (Ollama/Gemini)
- [ ] **M4** — Observability (Prometheus/Grafana)
- [ ] **M5** — CI, E2E, CV polish

---

## Contributing

This is a portfolio / learning project. Commits follow [Conventional Commits](https://www.conventionalcommits.org/). See `.cursor/rules/` for coding standards.

---

## License

MIT (or specify before public release)
