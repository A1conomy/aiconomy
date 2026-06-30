package com.aiconomy.market.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.aiconomy.market.domain.Trade;
import com.aiconomy.market.service.exception.SettlementFailedException;

/**
 * Settles executed trades via the ledger REST API (ADR-004: post-trade settlement).
 */
@Service
public class LedgerSettlementClient {

	private static final Logger log = LoggerFactory.getLogger(LedgerSettlementClient.class);

	private final RestClient ledgerRestClient;

	public LedgerSettlementClient(RestClient ledgerRestClient) {
		this.ledgerRestClient = ledgerRestClient;
	}

	public void settle(Trade trade) {
		log.info("Settling trade: id={} buyer={} seller={} amount={}",
				trade.id(), trade.buyerAccountId(), trade.sellerAccountId(), trade.settlementAmount());

		try {
			ledgerRestClient.post()
				.uri("/api/v1/transfers")
				.contentType(MediaType.APPLICATION_JSON)
				.body(new LedgerTransferRequest(
						trade.buyerAccountId(),
						trade.sellerAccountId(),
						trade.settlementAmount()))
				.retrieve()
				.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response) -> {
					String body = new String(response.getBody().readAllBytes());
					throw new SettlementFailedException(response.getStatusCode(), body);
				})
				.toBodilessEntity();
		}
		catch (SettlementFailedException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new SettlementFailedException(
					HttpStatus.SERVICE_UNAVAILABLE,
					"Ledger unavailable: " + ex.getMessage());
		}
	}

	private record LedgerTransferRequest(

			UUID fromAccountId,

			UUID toAccountId,

			BigDecimal amount

	) {
	}

}
