package com.aiconomy.market.service;

import java.util.List;
import java.util.Optional;

import com.aiconomy.market.domain.Order;
import com.aiconomy.market.domain.Trade;
import com.aiconomy.market.dto.SubmitOrderRequest;

/**
 * Result of submitting an order: trades produced and any unmatched remainder resting on the book.
 */
public record MatchResult(

		List<Trade> trades,

		Optional<Order> restingOrder

) {
}
