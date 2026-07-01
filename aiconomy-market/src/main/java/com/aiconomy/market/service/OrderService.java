package com.aiconomy.market.service;

import org.springframework.stereotype.Service;

import com.aiconomy.market.dto.SubmitOrderRequest;
import com.aiconomy.market.dto.SubmitOrderResponse;
import com.aiconomy.market.event.TradeEventPublisher;

/**
 * Orchestrates matching and post-trade ledger settlement.
 */
@Service
public class OrderService {

	private final MatchingEngine matchingEngine;

	private final LedgerSettlementClient ledgerSettlementClient;

	private final TradeEventPublisher tradeEventPublisher;

	public OrderService(
			MatchingEngine matchingEngine,
			LedgerSettlementClient ledgerSettlementClient,
			TradeEventPublisher tradeEventPublisher) {
		this.matchingEngine = matchingEngine;
		this.ledgerSettlementClient = ledgerSettlementClient;
		this.tradeEventPublisher = tradeEventPublisher;
	}

	public SubmitOrderResponse submitOrder(SubmitOrderRequest request) {
		MatchResult result = matchingEngine.submitOrder(request);
		result.trades().forEach(trade -> {
			ledgerSettlementClient.settle(trade);
			tradeEventPublisher.publish(trade);
		});
		return SubmitOrderResponse.from(result);
	}

}
