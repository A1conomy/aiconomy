package com.aiconomy.ledger.dto;

import java.math.BigDecimal;

import com.aiconomy.ledger.domain.AccountType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for opening a new ledger account.
 */
public record CreateAccountRequest(

		@NotBlank String ownerId,

		@NotNull AccountType accountType,

		@NotNull @DecimalMin(value = "0.00") BigDecimal initialBalance

) {
}
