package com.aiconomy.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionTemplate transactionTemplate;

	private TransferService transferService;

	private UUID sourceId;

	private UUID destinationId;

	private Account source;

	private Account destination;

	@BeforeEach
	void setUp() {
		transferService = new TransferService(accountRepository, transactionTemplate);
		sourceId = UUID.randomUUID();
		destinationId = UUID.randomUUID();
		source = new Account(sourceId, "consumer-1", AccountType.CONSUMER, new BigDecimal("100.00"));
		destination = new Account(destinationId, "firm-1", AccountType.FIRM, new BigDecimal("50.00"));

		doAnswer(invocation -> {
			Consumer<TransactionStatus> consumer = invocation.getArgument(0);
			consumer.accept(new SimpleTransactionStatus());
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());
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

		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> transferService.transfer(sourceId, destinationId, new BigDecimal("150.00")))
			.isInstanceOf(InsufficientFundsException.class);

		assertThat(source.getBalance()).isEqualByComparingTo("100.00");
		verify(accountRepository, never()).save(any());
	}

}
