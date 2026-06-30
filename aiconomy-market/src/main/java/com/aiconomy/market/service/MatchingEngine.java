package com.aiconomy.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.aiconomy.market.domain.Order;
import com.aiconomy.market.domain.OrderSide;
import com.aiconomy.market.domain.Trade;
import com.aiconomy.market.dto.SubmitOrderRequest;

/**
 * Price-time matching engine. Resting orders set the trade price; incoming orders are the taker.
 */
@Service
public class MatchingEngine {

	private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	private final OrderBookStore orderBookStore;

	public MatchingEngine(OrderBookStore orderBookStore) {
		this.orderBookStore = orderBookStore;
	}

	public MatchResult submitOrder(SubmitOrderRequest request) {
		Order incoming = Order.fromRequest(request);
		log.info("Order submitted: id={} side={} symbol={} price={} qty={}",
				incoming.id(), incoming.side(), incoming.symbol(), incoming.price(), incoming.quantity());

		List<Trade> trades = new ArrayList<>();
		OrderSide oppositeSide = incoming.side() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;

		while (incoming.remainingQuantity().compareTo(ZERO) > 0) {
			Optional<Order> restingOptional = orderBookStore.peekBestOrder(incoming.symbol(), oppositeSide);
			if (restingOptional.isEmpty() || !pricesCross(incoming, restingOptional.get())) {
				break;
			}

			Order resting = restingOptional.get();
			BigDecimal tradeQuantity = incoming.remainingQuantity().min(resting.remainingQuantity());
			BigDecimal tradePrice = resting.price();

			Trade trade = createTrade(incoming, resting, tradePrice, tradeQuantity);
			trades.add(trade);
			log.info("Trade executed: id={} price={} qty={} buyer={} seller={}",
					trade.id(), trade.price(), trade.quantity(), trade.buyerAccountId(), trade.sellerAccountId());

			BigDecimal restingRemaining = resting.remainingQuantity().subtract(tradeQuantity);
			if (restingRemaining.compareTo(ZERO) == 0) {
				orderBookStore.removeOrder(resting);
			}
			else {
				orderBookStore.updateRemainingQuantity(resting.id(), restingRemaining);
			}

			incoming = incoming.withRemainingQuantity(incoming.remainingQuantity().subtract(tradeQuantity));
		}

		Optional<Order> restingOrder = Optional.empty();
		if (incoming.remainingQuantity().compareTo(ZERO) > 0) {
			orderBookStore.addOrder(incoming);
			restingOrder = Optional.of(incoming);
		}

		return new MatchResult(List.copyOf(trades), restingOrder);
	}

	private static boolean pricesCross(Order incoming, Order resting) {
		if (incoming.side() == OrderSide.BUY) {
			return resting.price().compareTo(incoming.price()) <= 0;
		}
		return incoming.price().compareTo(resting.price()) <= 0;
	}

	private static Trade createTrade(Order incoming, Order resting, BigDecimal tradePrice, BigDecimal tradeQuantity) {
		Order buyOrder = incoming.side() == OrderSide.BUY ? incoming : resting;
		Order sellOrder = incoming.side() == OrderSide.SELL ? incoming : resting;

		return new Trade(
				UUID.randomUUID(),
				incoming.symbol(),
				tradePrice,
				tradeQuantity,
				buyOrder.id(),
				sellOrder.id(),
				buyOrder.accountId(),
				sellOrder.accountId(),
				Instant.now());
	}

}
