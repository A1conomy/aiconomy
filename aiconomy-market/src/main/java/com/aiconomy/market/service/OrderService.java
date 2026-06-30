package com.aiconomy.market.service;

import org.springframework.stereotype.Service;

import com.aiconomy.market.dto.SubmitOrderRequest;
import com.aiconomy.market.dto.SubmitOrderResponse;

/**
 * Orchestrates matching and post-trade ledger settlement.
 */
@Service
public class OrderService {

	private final MatchingEngine matchingEngine;

	private final LedgerSettlementClient ledgerSettlementClient;

	public OrderService(MatchingEngine matchingEngine, LedgerSettlementClient ledgerSettlementClient) {
		this.matchingEngine = matchingEngine;
		this.ledgerSettlementClient = ledgerSettlementClient;
	}

	public SubmitOrderResponse submitOrder(SubmitOrderRequest request) {
		MatchResult result = matchingEngine.submitOrder(request);
		result.trades().forEach(ledgerSettlementClient::settle);
		return SubmitOrderResponse.from(result);
	}

}
