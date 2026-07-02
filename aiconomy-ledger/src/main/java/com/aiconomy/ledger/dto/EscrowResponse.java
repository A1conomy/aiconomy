package com.aiconomy.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.aiconomy.ledger.domain.EscrowHold;
import com.aiconomy.ledger.domain.EscrowStatus;

/**
 * Response for escrow hold operations.
 */
public record EscrowResponse(

		UUID id,

		UUID fromAccountId,

		UUID toAccountId,

		UUID taskId,

		BigDecimal amount,

		EscrowStatus status

) {

	public static EscrowResponse from(EscrowHold hold) {
		return new EscrowResponse(
				hold.getId(),
				hold.getFromAccountId(),
				hold.getToAccountId(),
				hold.getTaskId(),
				hold.getAmount(),
				hold.getStatus());
	}

}
