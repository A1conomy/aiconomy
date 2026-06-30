package com.aiconomy.market.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aiconomy.market.service.MatchResult;

/**
 * Response after submitting an order: executed trades and any resting remainder.
 */
public record SubmitOrderResponse(

		List<TradeResponse> trades,

		OrderResponse restingOrder

) {

	public static SubmitOrderResponse from(MatchResult result) {
		List<TradeResponse> trades = result.trades().stream()
			.map(TradeResponse::from)
			.toList();
		OrderResponse restingOrder = result.restingOrder()
			.map(OrderResponse::from)
			.orElse(null);
		return new SubmitOrderResponse(trades, restingOrder);
	}

}
