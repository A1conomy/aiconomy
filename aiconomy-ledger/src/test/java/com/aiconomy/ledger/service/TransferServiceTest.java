package com.aiconomy.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.service.exception.AccountNotFoundException;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	@Mock
	private AccountRepository accountRepository;

	private TransferService transferService;

	private UUID sourceId;

	private UUID destinationId;

	private Account source;

	private Account destination;

	@BeforeEach
	void setUp() {
		transferService = new TransferService(accountRepository);
		sourceId = UUID.randomUUID();
		destinationId = UUID.randomUUID();
		source = new Account(sourceId, "consumer-1", AccountType.CONSUMER, new BigDecimal("100.00"));
		destination = new Account(destinationId, "firm-1", AccountType.FIRM, new BigDecimal("50.00"));
	}

	@Test
	void transferMovesBalanceBetweenAccounts() {
		when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
		when(accountRepository.findById(destinationId)).thenReturn(Optional.of(destination));
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

		transferService.transfer(sourceId, destinationId, new BigDecimal("30.00"));

		assertThat(source.getBalance()).isEqualByComparingTo("70.00");
		assertThat(destination.getBalance()).isEqualByComparingTo("80.00");
		verify(accountRepository).save(source);
		verify(accountRepository).save(destination);
	}

	@Test
	void transferFailsWhenInsufficientFunds() {
		when(accountRepository.findById(sourceId)).thenReturn(Optional.of(source));
		when(accountRepository.findById(destinationId)).thenReturn(Optional.of(destination));

		assertThatThrownBy(() -> transferService.transfer(sourceId, destinationId, new BigDecimal("150.00")))
			.isInstanceOf(InsufficientFundsException.class);

		assertThat(source.getBalance()).isEqualByComparingTo("100.00");
		verify(accountRepository, never()).save(any());
	}

	@Test
	void transferFailsWhenAccountNotFound() {
		when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> transferService.transfer(sourceId, destinationId, new BigDecimal("10.00")))
			.isInstanceOf(AccountNotFoundException.class);
	}

}
