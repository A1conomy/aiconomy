package com.aiconomy.tasks.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for ledger escrow operations during task lifecycle.
 */
@Component
public class LedgerEscrowClient {

	private final RestClient ledgerRestClient;

	public LedgerEscrowClient(RestClient ledgerRestClient) {
		this.ledgerRestClient = ledgerRestClient;
	}

	public UUID hold(UUID fromAccountId, UUID toAccountId, UUID taskId, BigDecimal amount) {
		EscrowPayload response = ledgerRestClient.post()
			.uri("/api/v1/escrow/hold")
			.body(new HoldPayload(fromAccountId, toAccountId, taskId, amount))
			.retrieve()
			.body(EscrowPayload.class);
		if (response == null || response.id() == null) {
			throw new IllegalStateException("Ledger escrow hold returned empty response");
		}
		return response.id();
	}

	public void release(UUID escrowHoldId) {
		ledgerRestClient.post()
			.uri("/api/v1/escrow/{escrowHoldId}/release", escrowHoldId)
			.retrieve()
			.toBodilessEntity();
	}

	public void refund(UUID escrowHoldId) {
		ledgerRestClient.post()
			.uri("/api/v1/escrow/{escrowHoldId}/refund", escrowHoldId)
			.retrieve()
			.toBodilessEntity();
	}

	private record HoldPayload(UUID fromAccountId, UUID toAccountId, UUID taskId, BigDecimal amount) {
	}

	private record EscrowPayload(UUID id) {
	}

}
