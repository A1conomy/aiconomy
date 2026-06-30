package com.aiconomy.ledger.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for an ACID transfer between two accounts.
 */
public record TransferRequest(

		@NotNull UUID fromAccountId,

		@NotNull UUID toAccountId,

		@NotNull @DecimalMin(value = "0.01") BigDecimal amount

) {
}
