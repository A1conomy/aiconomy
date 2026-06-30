package com.aiconomy.market.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.market.domain.Order;
import com.aiconomy.market.domain.OrderSide;

/**
 * Public view of an order returned by the market API.
 */
public record OrderResponse(

		UUID id,

		UUID accountId,

		String symbol,

		OrderSide side,

		BigDecimal price,

		BigDecimal quantity,

		BigDecimal remainingQuantity,

		Instant createdAt

) {

	public static OrderResponse from(Order order) {
		return new OrderResponse(
				order.id(),
				order.accountId(),
				order.symbol(),
				order.side(),
				order.price(),
				order.quantity(),
				order.remainingQuantity(),
				order.createdAt());
	}

}
