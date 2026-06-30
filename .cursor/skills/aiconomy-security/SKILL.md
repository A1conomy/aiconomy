---
name: aiconomy-security
description: Runs security review on AIconomy code changes. Use when adding REST endpoints, auth, secrets handling, dependencies, Docker configs, or before milestone merges. Triggers security-review subagent on local diff.
---

# AIconomy Security Review

## When to Run

- New or changed REST/Kafka handlers
- Auth, CORS, or actuator exposure
- `.env`, credentials, or config loading
- New dependencies (Gradle or pip)
- Before completing a milestone merge to `main`
- User asks for security check

## How to Run

Launch one `security-review` subagent:

- `readonly: true`
- `run_in_background: false`
- `description: "Security Review"`
- `subagent_type: "security-review"`

Prompt shape:

```text
Full Repository Path: <absolute path to aiconomy repo>
Diff: branch changes
Custom Instructions: AIconomy fintech simulation — check for secret leakage, injection, insecure defaults, missing input validation, Kafka event trust boundaries.
```

Default `Diff` to `branch changes`. Use `uncommitted changes` only when reviewing dirty working tree.

## AIconomy-Specific Checklist

After subagent completes, verify:

- [ ] No secrets in git-tracked files
- [ ] Money operations use BigDecimal, not float
- [ ] Ledger invariants enforced server-side (not only client)
- [ ] Kafka events validated before processing (schema + idempotency)
- [ ] Python agents: API keys from env only
- [ ] Docker Compose: default passwords documented as dev-only in README

## Report Format

Summarize findings as markdown table: Severity | Location (file:line) | Finding

Do not auto-fix unless user requests.
