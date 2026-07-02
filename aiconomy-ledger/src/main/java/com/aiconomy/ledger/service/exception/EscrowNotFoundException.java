package com.aiconomy.ledger.service.exception;

import java.util.UUID;

/**
 * Thrown when an escrow hold cannot be found or is not active.
 */
public class EscrowNotFoundException extends RuntimeException {

	public EscrowNotFoundException(UUID escrowHoldId) {
		super("Escrow hold not found or not active: " + escrowHoldId);
	}

}
