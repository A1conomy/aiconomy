# AIconomy — Architecture Decision Records

This document records key architectural decisions. Update when making significant design choices.

---

## ADR-001: Gradle Multi-Project Monorepo

**Status:** Accepted  
**Context:** Single developer, internship CV project, Java + Python agents.  
**Decision:** One GitHub org repo with Gradle subprojects + `agents/` folder.  
**Alternatives:** Maven multi-module; separate repos for Java/Python.  
**Rationale:** Atomic commits, unified CI, one README link for CV.

---

## ADR-002: Kafka + Redis Split

**Status:** Superseded by ADR-005  
**Context:** Need durable events and fast order matching.  
**Decision:** Kafka = event journal; Redis = hot order book projection.  
**Rationale:** Clear CAP story at interviews — Postgres/Kafka CP, Redis AP for matching latency.

---

## ADR-003: LLM Provider Abstraction

**Status:** Accepted  
**Context:** Dev cost vs demo quality.  
**Decision:** Strategy pattern — Ollama (dev), Gemini (prod), `LLM_PROVIDER` env switch.  
**Alternatives:** Hardcode one vendor; Spring AI in Java only.  
**Rationale:** Unlimited local iteration; swap without agent refactor.

---

## ADR-004: Settlement Post-Trade

**Status:** Superseded by ADR-005  
**Context:** Avoid double-spend between market and ledger.  
**Decision:** Match in Redis → emit `TradeExecuted` → ledger settles ACID.  
**Rationale:** Simpler agent flow; ledger remains single source of truth for balances.

---

## ADR-005: Task Marketplace & Manager-Worker Model

**Status:** Accepted (2026-07-01)  
**Context:** WIDGET buy/sell simulation lacked economic motivation — no negotiation, specialization, or emergent dynamics.  
**Decision:** Pivot to a **freelancing services economy**:

- **ClientAgent** posts projects and accepts/rejects deliveries
- **ManagerAgent** negotiates price with clients, hires workers, orchestrates delivery
- **WorkerAgent** claims specialized tasks, delivers, subcontracts peers
- **Ledger** provides payment rail + **escrow** (hold on accept, release on delivery approval)
- **Kafka** carries task lifecycle and payment negotiation events
- Retire `aiconomy-market` WIDGET order book

**Alternatives:** Keep dual economy (goods + services); pure P2P without managers.  
**Rationale:** Mirrors real freelancing/agency dynamics; strong fit for LangGraph multi-agent orchestration; ledger becomes infrastructure, not the product.

### Task lifecycle (target)

```
OPEN → CLAIMED → DELIVERED → ACCEPTED | REJECTED
```

Escrow: hold funds when offer accepted → release on `tasks.accepted` → refund on reject (MVP).

### Kafka topics

`tasks.posted`, `tasks.claimed`, `tasks.delivered`, `tasks.accepted`, `tasks.rejected`, `payments.proposed`, `payments.accepted`

---

## Module Map (target)

```
aiconomy/
├── aiconomy-common/     # Events, DTOs, KafkaTopics
├── aiconomy-ledger/     # :8081 — accounts, transfers, escrow
├── aiconomy-tasks/      # :8082 — task board + lifecycle
├── aiconomy-analytics/  # :8083 — macro metrics
├── agents/              # client_agent, manager_agent, worker_agent
└── infra/               # Prometheus, Grafana configs (planned)
```

*Update this document as modules are implemented.*
