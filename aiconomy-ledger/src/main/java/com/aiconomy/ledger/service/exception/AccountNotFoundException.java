package com.aiconomy.ledger.service.exception;

import java.util.UUID;

/**
 * Thrown when a transfer references an account that does not exist.
 */
public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException(UUID accountId) {
		super("Account not found: " + accountId);
	}

}
