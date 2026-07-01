#!/usr/bin/env bash
# End-to-end ledger flow against a running ledger service.
#
# Prerequisites:
#   docker-compose up -d
#   ./gradlew :aiconomy-ledger:bootRun
#
# Usage: ./infra/scripts/e2e-ledger.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LEDGER_URL="${LEDGER_URL:-http://localhost:8081}"
RED="\033[0;31m"
GREEN="\033[0;32m"
NC="\033[0m"

pass() { echo -e "${GREEN}✓${NC} $1"; }
fail() { echo -e "${RED}✗${NC} $1"; exit 1; }

json_field() {
	python3 -c "import json,sys; print(json.load(sys.stdin)['$2'])" <<< "$1"
}

http_code() {
	curl -s -o /dev/null -w "%{http_code}" "$1"
}

echo "=== AIconomy E2E ledger test ==="

[[ "$(http_code "${LEDGER_URL}/actuator/health")" == "200" ]] \
	|| fail "Ledger not reachable at ${LEDGER_URL} (run :aiconomy-ledger:bootRun)"
pass "Ledger health OK"

PAYER_RESP=$(curl -s -X POST "${LEDGER_URL}/api/v1/accounts" \
	-H "Content-Type: application/json" \
	-d '{"ownerId":"e2e-client","accountType":"CLIENT","initialBalance":1000.00}')
PAYER_ID=$(json_field "$PAYER_RESP" id)
pass "Client account created (${PAYER_ID})"

PAYEE_RESP=$(curl -s -X POST "${LEDGER_URL}/api/v1/accounts" \
	-H "Content-Type: application/json" \
	-d '{"ownerId":"e2e-manager","accountType":"MANAGER","initialBalance":0.00}')
PAYEE_ID=$(json_field "$PAYEE_RESP" id)
pass "Manager account created (${PAYEE_ID})"

curl -s -X POST "${LEDGER_URL}/api/v1/transfers" \
	-H "Content-Type: application/json" \
	-d "{\"fromAccountId\":\"${PAYER_ID}\",\"toAccountId\":\"${PAYEE_ID}\",\"amount\":150.00}" \
	> /dev/null
pass "Transfer executed (150.00)"

PAYER_BAL=$(json_field "$(curl -s "${LEDGER_URL}/api/v1/accounts/${PAYER_ID}")" balance)
PAYEE_BAL=$(json_field "$(curl -s "${LEDGER_URL}/api/v1/accounts/${PAYEE_ID}")" balance)
python3 -c "import sys; assert float(sys.argv[1]) == 850.0" "$PAYER_BAL" \
	|| fail "Expected client balance 850.00, got ${PAYER_BAL}"
python3 -c "import sys; assert float(sys.argv[1]) == 150.0" "$PAYEE_BAL" \
	|| fail "Expected manager balance 150.00, got ${PAYEE_BAL}"
pass "Ledger balances correct (client=850.00, manager=150.00)"

echo ""
echo -e "${GREEN}E2E ledger test passed.${NC}"
