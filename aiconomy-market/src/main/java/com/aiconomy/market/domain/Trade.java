package com.aiconomy.market.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * An executed match between a bid and an ask.
 */
public record Trade(

		UUID id,

		String symbol,

		BigDecimal price,

		BigDecimal quantity,

		UUID buyOrderId,

		UUID sellOrderId,

		UUID buyerAccountId,

		UUID sellerAccountId,

		Instant executedAt

) {

	public BigDecimal settlementAmount() {
		return price.multiply(quantity);
	}

}
