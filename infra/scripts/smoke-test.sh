#!/usr/bin/env bash
# Verifies local infrastructure is healthy.
# Usage: ./infra/scripts/smoke-test.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

POSTGRES_USER="${POSTGRES_USER:-aiconomy}"
POSTGRES_DB="${POSTGRES_DB:-aiconomy}"
RED="\033[0;31m"
GREEN="\033[0;32m"
NC="\033[0m"

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }

echo "=== AIconomy infrastructure smoke test ==="

# Docker Compose services running
docker-compose ps | grep -q aiconomy-postgres || fail "Postgres container not running"
docker-compose ps | grep -q aiconomy-redis || fail "Redis container not running"
docker-compose ps | grep -q aiconomy-kafka || fail "Kafka container not running"
pass "Docker containers are running"

# PostgreSQL
docker-compose exec -T postgres pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null \
  || fail "PostgreSQL not accepting connections"
pass "PostgreSQL accepts connections"

# Redis
docker-compose exec -T redis redis-cli ping | grep -q PONG \
  || fail "Redis did not respond with PONG"
pass "Redis responds to PING"

# Kafka — list topics
TOPICS=$(docker-compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 --list)
for topic in tasks.posted tasks.claimed tasks.delivered tasks.accepted tasks.rejected payments.proposed payments.accepted ledger.commands ledger.events macro.snapshots simulation.tick; do
  echo "$TOPICS" | grep -q "^${topic}$" || fail "Missing Kafka topic: ${topic}"
done
pass "Kafka topics exist (11/11)"

# Kafka — produce & consume test message
TEST_TOPIC="infra.smoke-test"
docker-compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:29092 \
  --create --if-not-exists --topic "$TEST_TOPIC" --partitions 1 --replication-factor 1 >/dev/null

echo "smoke-test-$(date +%s)" | docker-compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:29092 --topic "$TEST_TOPIC" >/dev/null

CONSUMED=$(timeout 10 docker-compose exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:29092 \
  --topic "$TEST_TOPIC" \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 5000 2>/dev/null | tail -1)

[[ -n "$CONSUMED" ]] || fail "Kafka produce/consume test failed"
pass "Kafka produce/consume works"

echo ""
echo -e "${GREEN}All smoke tests passed.${NC}"
