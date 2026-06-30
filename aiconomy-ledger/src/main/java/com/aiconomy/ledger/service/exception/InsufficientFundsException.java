package com.aiconomy.ledger.service.exception;

import java.util.UUID;

/**
 * Thrown when a debit would result in a negative balance.
 */
public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(UUID accountId) {
		super("Insufficient funds on account: " + accountId);
	}

}
