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

**Status:** Accepted  
**Context:** Need durable events and fast order matching.  
**Decision:** Kafka = event journal; Redis = hot order book projection.  
**Alternatives:** Kafka-only; Redis Streams as sole broker.  
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

**Status:** Accepted  
**Context:** Avoid double-spend between market and ledger.  
**Decision:** Match in Redis → emit `TradeExecuted` → ledger settles ACID.  
**Alternatives:** Reserve funds before order submit.  
**Rationale:** Simpler agent flow; ledger remains single source of truth for balances.

---

## Module Map (target)

```
aiconomy/
├── aiconomy-common/     # Events, DTOs
├── aiconomy-ledger/     # :8081
├── aiconomy-market/     # :8082
├── aiconomy-analytics/  # :8083
├── agents/              # Python LangGraph
└── infra/               # Prometheus, Grafana configs
```

*Update this document as modules are implemented.*

### M2 settlement flow (implemented)

```
POST /api/v1/orders  →  MatchingEngine (Redis)
                     →  LedgerSettlementClient (HTTP POST /transfers)
```

Kafka event publishing is planned as a follow-up; REST API is the current entry point for agents and curl testing.
