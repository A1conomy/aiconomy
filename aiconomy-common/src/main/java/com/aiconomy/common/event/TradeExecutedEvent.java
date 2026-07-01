package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka payload emitted after a trade is matched and settled.
 */
public record TradeExecutedEvent(

		UUID tradeId,

		String symbol,

		BigDecimal price,

		BigDecimal quantity,

		UUID buyOrderId,

		UUID sellOrderId,

		UUID buyerAccountId,

		UUID sellerAccountId,

		BigDecimal settlementAmount,

		Instant executedAt

) {
}
