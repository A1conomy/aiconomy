package com.aiconomy.ledger.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.EscrowHold;
import com.aiconomy.ledger.dto.EscrowResponse;
import com.aiconomy.ledger.dto.HoldEscrowRequest;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.repository.EscrowHoldRepository;
import com.aiconomy.ledger.service.exception.EscrowNotFoundException;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;
import com.aiconomy.ledger.service.exception.InvalidTransferAmountException;

/**
 * Locks client funds on task claim and releases or refunds on client decision.
 */
@Service
public class EscrowService {

	private static final Logger log = LoggerFactory.getLogger(EscrowService.class);

	private static final int MAX_ATTEMPTS = 3;

	private final AccountRepository accountRepository;

	private final EscrowHoldRepository escrowHoldRepository;

	private final TransactionTemplate transactionTemplate;

	public EscrowService(
			AccountRepository accountRepository,
			EscrowHoldRepository escrowHoldRepository,
			TransactionTemplate transactionTemplate) {
		this.accountRepository = accountRepository;
		this.escrowHoldRepository = escrowHoldRepository;
		this.transactionTemplate = transactionTemplate;
	}

	public EscrowResponse hold(HoldEscrowRequest request) {
		validateAmount(request.amount());
		log.info("Escrow hold started: task={} from={} to={} amount={}",
				request.taskId(), request.fromAccountId(), request.toAccountId(), request.amount());

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				EscrowHold hold = transactionTemplate.execute(status -> executeHold(request));
				log.info("Escrow hold created: id={} task={}", hold.getId(), hold.getTaskId());
				return EscrowResponse.from(hold);
			}
			catch (OptimisticLockingFailureException ex) {
				log.warn("Optimistic lock conflict on escrow hold attempt={}/{}", attempt, MAX_ATTEMPTS);
				if (attempt == MAX_ATTEMPTS) {
					throw ex;
				}
			}
		}
		throw new IllegalStateException("Escrow hold failed after retries");
	}

	public EscrowResponse release(UUID escrowHoldId) {
		log.info("Escrow release started: id={}", escrowHoldId);
		return mutateEscrow(escrowHoldId, EscrowMutation.RELEASE);
	}

	public EscrowResponse refund(UUID escrowHoldId) {
		log.info("Escrow refund started: id={}", escrowHoldId);
		return mutateEscrow(escrowHoldId, EscrowMutation.REFUND);
	}

	private EscrowResponse mutateEscrow(UUID escrowHoldId, EscrowMutation mutation) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				EscrowHold hold = transactionTemplate.execute(status -> executeMutation(escrowHoldId, mutation));
				log.info("Escrow {} completed: id={}", mutation.name().toLowerCase(), hold.getId());
				return EscrowResponse.from(hold);
			}
			catch (OptimisticLockingFailureException ex) {
				log.warn("Optimistic lock conflict on escrow {} attempt={}/{}", mutation, attempt, MAX_ATTEMPTS);
				if (attempt == MAX_ATTEMPTS) {
					throw ex;
				}
			}
		}
		throw new IllegalStateException("Escrow mutation failed after retries");
	}

	private EscrowHold executeHold(HoldEscrowRequest request) {
		Account source = loadAccount(request.fromAccountId());
		if (!source.hasSufficientFunds(request.amount())) {
			throw new InsufficientFundsException(request.fromAccountId());
		}

		source.debit(request.amount());
		accountRepository.save(source);

		EscrowHold hold = new EscrowHold(
				UUID.randomUUID(),
				request.fromAccountId(),
				request.toAccountId(),
				request.taskId(),
				request.amount());
		return escrowHoldRepository.save(hold);
	}

	private EscrowHold executeMutation(UUID escrowHoldId, EscrowMutation mutation) {
		EscrowHold hold = escrowHoldRepository.findById(escrowHoldId)
			.filter(EscrowHold::isActive)
			.orElseThrow(() -> new EscrowNotFoundException(escrowHoldId));

		Account account = switch (mutation) {
			case RELEASE -> {
				hold.markReleased();
				yield loadAccount(hold.getToAccountId());
			}
			case REFUND -> {
				hold.markRefunded();
				yield loadAccount(hold.getFromAccountId());
			}
		};

		account.credit(hold.getAmount());
		accountRepository.save(account);
		return escrowHoldRepository.save(hold);
	}

	private void validateAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidTransferAmountException();
		}
	}

	private Account loadAccount(UUID accountId) {
		return accountRepository.findById(accountId)
			.orElseThrow(() -> new com.aiconomy.ledger.service.exception.AccountNotFoundException(accountId));
	}

	private enum EscrowMutation {
		RELEASE,
		REFUND
	}

}
