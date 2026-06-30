package com.aiconomy.market.service.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Thrown when the ledger rejects or fails a post-trade settlement transfer.
 */
public class SettlementFailedException extends RuntimeException {

	private final HttpStatusCode statusCode;

	public SettlementFailedException(HttpStatusCode statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

}
