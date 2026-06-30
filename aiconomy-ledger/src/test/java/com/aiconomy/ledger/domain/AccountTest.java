package com.aiconomy.ledger.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AccountTest {

	@Test
	void constructorSetsInitialBalance() {
		UUID id = UUID.randomUUID();

		Account account = new Account(id, "agent-1", AccountType.CONSUMER, new BigDecimal("100.00"));

		assertThat(account.getId()).isEqualTo(id);
		assertThat(account.getOwnerId()).isEqualTo("agent-1");
		assertThat(account.getAccountType()).isEqualTo(AccountType.CONSUMER);
		assertThat(account.getBalance()).isEqualByComparingTo("100.00");
	}

}
