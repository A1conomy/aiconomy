package com.aiconomy.ledger.service.exception;

/**
 * Thrown when transfer amount is zero or negative.
 */
public class InvalidTransferAmountException extends RuntimeException {

	public InvalidTransferAmountException() {
		super("Transfer amount must be positive");
	}

}
