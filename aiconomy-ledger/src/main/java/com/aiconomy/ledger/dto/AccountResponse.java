package com.aiconomy.ledger.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;

/**
 * Public view of an account returned by the REST API.
 */
public record AccountResponse(
		UUID id,
		String ownerId,
		AccountType accountType,
		BigDecimal balance,
		Long version,
		Instant createdAt,
		Instant updatedAt) {

	public static AccountResponse from(Account account) {
		return new AccountResponse(
				account.getId(),
				account.getOwnerId(),
				account.getAccountType(),
				account.getBalance(),
				account.getVersion(),
				account.getCreatedAt(),
				account.getUpdatedAt());
	}

}
