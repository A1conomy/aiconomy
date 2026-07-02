package com.aiconomy.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for locking client funds in escrow when a task is claimed.
 */
public record HoldEscrowRequest(

		@NotNull UUID fromAccountId,

		@NotNull UUID toAccountId,

		@NotNull UUID taskId,

		@NotNull @DecimalMin(value = "0.01") BigDecimal amount

) {
}
