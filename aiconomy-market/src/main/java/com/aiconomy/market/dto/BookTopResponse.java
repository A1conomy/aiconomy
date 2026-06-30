package com.aiconomy.market.dto;

import java.math.BigDecimal;

/**
 * Top-of-book snapshot for a symbol (best bid / best ask).
 */
public record BookTopResponse(

		String symbol,

		BigDecimal bestBid,

		BigDecimal bestAsk

) {
}
