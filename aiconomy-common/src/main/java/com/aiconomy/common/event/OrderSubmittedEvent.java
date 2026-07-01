package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kafka payload when an agent or service submits an order to the market.
 */
public record OrderSubmittedEvent(

		UUID eventId,

		UUID accountId,

		String symbol,

		String side,

		BigDecimal price,

		BigDecimal quantity,

		Instant submittedAt

) {
}
