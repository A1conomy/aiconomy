package com.aiconomy.market.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.market.dto.SubmitOrderRequest;

/**
 * A limit order resting in or entering the order book.
 * {@code remainingQuantity} supports partial fills in the matching engine.
 */
public record Order(

		UUID id,

		UUID accountId,

		String symbol,

		OrderSide side,

		BigDecimal price,

		BigDecimal quantity,

		BigDecimal remainingQuantity,

		Instant createdAt

) {

	public Order withRemainingQuantity(BigDecimal remainingQuantity) {
		return new Order(id, accountId, symbol, side, price, quantity, remainingQuantity, createdAt);
	}

	public static Order fromRequest(SubmitOrderRequest request) {
		Instant now = Instant.now();
		return new Order(
				UUID.randomUUID(),
				request.accountId(),
				request.symbol(),
				request.side(),
				request.price(),
				request.quantity(),
				request.quantity(),
				now);
	}

}
