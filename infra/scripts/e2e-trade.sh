#!/usr/bin/env bash
# End-to-end trade flow against running ledger + market services.
#
# Prerequisites:
#   docker-compose up -d
#   ./gradlew :aiconomy-ledger:bootRun   (terminal 1)
#   ./gradlew :aiconomy-market:bootRun   (terminal 2)
#
# Usage: ./infra/scripts/e2e-trade.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

LEDGER_URL="${LEDGER_URL:-http://localhost:8081}"
MARKET_URL="${MARKET_URL:-http://localhost:8082}"
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

echo "=== AIconomy E2E trade test ==="

[[ "$(http_code "${LEDGER_URL}/actuator/health")" == "200" ]] \
	|| fail "Ledger not reachable at ${LEDGER_URL} (run :aiconomy-ledger:bootRun)"
pass "Ledger health OK"

[[ "$(http_code "${MARKET_URL}/actuator/health")" == "200" ]] \
	|| fail "Market not reachable at ${MARKET_URL} (run :aiconomy-market:bootRun)"
pass "Market health OK"

SELLER_RESP=$(curl -s -X POST "${LEDGER_URL}/api/v1/accounts" \
	-H "Content-Type: application/json" \
	-d '{"ownerId":"e2e-seller","accountType":"FIRM","initialBalance":0.00}')
SELLER_ID=$(json_field "$SELLER_RESP" id)
pass "Seller account created (${SELLER_ID})"

BUYER_RESP=$(curl -s -X POST "${LEDGER_URL}/api/v1/accounts" \
	-H "Content-Type: application/json" \
	-d '{"ownerId":"e2e-buyer","accountType":"CONSUMER","initialBalance":500.00}')
BUYER_ID=$(json_field "$BUYER_RESP" id)
pass "Buyer account created (${BUYER_ID})"

SELL_ORDER=$(curl -s -X POST "${MARKET_URL}/api/v1/orders" \
	-H "Content-Type: application/json" \
	-d "{\"accountId\":\"${SELLER_ID}\",\"symbol\":\"WIDGET\",\"side\":\"SELL\",\"price\":10.00,\"quantity\":5.00}")
TRADES_AFTER_SELL=$(python3 -c "import json,sys; print(len(json.load(sys.stdin)['trades']))" <<< "$SELL_ORDER")
[[ "$TRADES_AFTER_SELL" == "0" ]] || fail "Resting sell should not produce trades"
pass "Resting sell order placed (5 WIDGET @ 10.00)"

TOP=$(curl -s "${MARKET_URL}/api/v1/market/WIDGET/top")
BEST_ASK=$(python3 -c "import json,sys; print(json.load(sys.stdin)['bestAsk'])" <<< "$TOP")
python3 -c "import sys; assert float(sys.argv[1]) == 10.0" "$BEST_ASK" \
	|| fail "Expected bestAsk=10.0, got ${BEST_ASK}"
pass "Top of book shows ask @ 10.00"

BUY_ORDER=$(curl -s -X POST "${MARKET_URL}/api/v1/orders" \
	-H "Content-Type: application/json" \
	-d "{\"accountId\":\"${BUYER_ID}\",\"symbol\":\"WIDGET\",\"side\":\"BUY\",\"price\":12.00,\"quantity\":3.00}")
TRADE_QTY=$(python3 -c "import json,sys; t=json.load(sys.stdin)['trades']; assert len(t)==1; print(t[0]['quantity'])" <<< "$BUY_ORDER")
SETTLEMENT=$(python3 -c "import json,sys; t=json.load(sys.stdin)['trades']; print(t[0]['settlementAmount'])" <<< "$BUY_ORDER")
[[ "$TRADE_QTY" == "3.00" || "$TRADE_QTY" == "3.0" ]] || fail "Expected trade qty 3, got ${TRADE_QTY}"
python3 -c "import sys; assert float(sys.argv[1]) == 30.0" "$SETTLEMENT" \
	|| fail "Expected settlement 30.0, got ${SETTLEMENT}"
pass "Matching buy executed (3 WIDGET @ 10.00, settlement 30.00)"

BUYER_BAL=$(json_field "$(curl -s "${LEDGER_URL}/api/v1/accounts/${BUYER_ID}")" balance)
SELLER_BAL=$(json_field "$(curl -s "${LEDGER_URL}/api/v1/accounts/${SELLER_ID}")" balance)
python3 -c "import sys; assert float(sys.argv[1]) == 470.0" "$BUYER_BAL" \
	|| fail "Expected buyer balance 470.00, got ${BUYER_BAL}"
python3 -c "import sys; assert float(sys.argv[1]) == 30.0" "$SELLER_BAL" \
	|| fail "Expected seller balance 30.00, got ${SELLER_BAL}"
pass "Ledger balances correct (buyer=470.00, seller=30.00)"

echo ""
echo -e "${GREEN}E2E trade test passed.${NC}"
