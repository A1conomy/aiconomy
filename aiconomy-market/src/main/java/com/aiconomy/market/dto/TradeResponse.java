package com.aiconomy.market.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.aiconomy.market.domain.Trade;

/**
 * Public view of an executed trade.
 */
public record TradeResponse(

		UUID id,

		String symbol,

		BigDecimal price,

		BigDecimal quantity,

		UUID buyerAccountId,

		UUID sellerAccountId,

		BigDecimal settlementAmount

) {

	public static TradeResponse from(Trade trade) {
		return new TradeResponse(
				trade.id(),
				trade.symbol(),
				trade.price(),
				trade.quantity(),
				trade.buyerAccountId(),
				trade.sellerAccountId(),
				trade.settlementAmount());
	}

}
