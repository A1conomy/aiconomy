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
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;
import com.aiconomy.ledger.domain.EscrowHold;
import com.aiconomy.ledger.domain.EscrowStatus;
import com.aiconomy.ledger.dto.HoldEscrowRequest;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.repository.EscrowHoldRepository;
import com.aiconomy.ledger.service.exception.EscrowNotFoundException;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private EscrowHoldRepository escrowHoldRepository;

	@Mock
	private TransactionTemplate transactionTemplate;

	private EscrowService escrowService;

	private UUID clientId;

	private UUID workerId;

	private UUID taskId;

	private Account client;

	private Account worker;

	@BeforeEach
	void setUp() {
		escrowService = new EscrowService(accountRepository, escrowHoldRepository, transactionTemplate);
		clientId = UUID.randomUUID();
		workerId = UUID.randomUUID();
		taskId = UUID.randomUUID();
		client = new Account(clientId, "client-1", AccountType.CLIENT, new BigDecimal("500.00"));
		worker = new Account(workerId, "worker-1", AccountType.WORKER, BigDecimal.ZERO);

		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			var callback = invocation.<org.springframework.transaction.support.TransactionCallback<EscrowHold>>getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});
	}

	@Test
	void holdDebitsClientAndCreatesActiveEscrow() {
		when(accountRepository.findById(clientId)).thenReturn(Optional.of(client));
		when(escrowHoldRepository.save(any(EscrowHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = escrowService.hold(new HoldEscrowRequest(clientId, workerId, taskId, new BigDecimal("150.00")));

		assertThat(client.getBalance()).isEqualByComparingTo("350.00");
		assertThat(response.status()).isEqualTo(EscrowStatus.ACTIVE);
		assertThat(response.amount()).isEqualByComparingTo("150.00");
	}

	@Test
	void holdFailsWhenInsufficientFunds() {
		when(accountRepository.findById(clientId)).thenReturn(Optional.of(client));

		assertThatThrownBy(() -> escrowService.hold(
				new HoldEscrowRequest(clientId, workerId, taskId, new BigDecimal("600.00"))))
			.isInstanceOf(InsufficientFundsException.class);

		verify(escrowHoldRepository, never()).save(any());
	}

	@Test
	void releaseCreditsWorker() {
		UUID escrowId = UUID.randomUUID();
		EscrowHold hold = new EscrowHold(escrowId, clientId, workerId, taskId, new BigDecimal("150.00"));
		when(escrowHoldRepository.findById(escrowId)).thenReturn(Optional.of(hold));
		when(accountRepository.findById(workerId)).thenReturn(Optional.of(worker));
		when(escrowHoldRepository.save(any(EscrowHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = escrowService.release(escrowId);

		assertThat(worker.getBalance()).isEqualByComparingTo("150.00");
		assertThat(response.status()).isEqualTo(EscrowStatus.RELEASED);
	}

	@Test
	void refundCreditsClient() {
		UUID escrowId = UUID.randomUUID();
		EscrowHold hold = new EscrowHold(escrowId, clientId, workerId, taskId, new BigDecimal("150.00"));
		client.debit(new BigDecimal("150.00"));
		when(escrowHoldRepository.findById(escrowId)).thenReturn(Optional.of(hold));
		when(accountRepository.findById(clientId)).thenReturn(Optional.of(client));
		when(escrowHoldRepository.save(any(EscrowHold.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = escrowService.refund(escrowId);

		assertThat(client.getBalance()).isEqualByComparingTo("500.00");
		assertThat(response.status()).isEqualTo(EscrowStatus.REFUNDED);
	}

	@Test
	void releaseFailsWhenEscrowNotActive() {
		UUID escrowId = UUID.randomUUID();
		when(escrowHoldRepository.findById(escrowId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> escrowService.release(escrowId))
			.isInstanceOf(EscrowNotFoundException.class);
	}

}
