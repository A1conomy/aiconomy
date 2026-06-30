package com.aiconomy.ledger.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.service.exception.AccountNotFoundException;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;
import com.aiconomy.ledger.service.exception.InvalidTransferAmountException;

/**
 * Executes ACID transfers between two accounts with optimistic-lock retry.
 */
@Service
public class TransferService {

	private static final Logger log = LoggerFactory.getLogger(TransferService.class);

	private static final int MAX_ATTEMPTS = 3;

	private final AccountRepository accountRepository;

	private final TransactionTemplate transactionTemplate;

	public TransferService(AccountRepository accountRepository, TransactionTemplate transactionTemplate) {
		this.accountRepository = accountRepository;
		this.transactionTemplate = transactionTemplate;
	}

	/**
	 * Moves funds from source to destination atomically.
	 * Retries on optimistic lock conflicts up to {@link #MAX_ATTEMPTS} times.
	 */
	public void transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
		log.info("Transfer started: from={} to={} amount={}", fromAccountId, toAccountId, amount);

		validateAmount(amount);
		validateDifferentAccounts(fromAccountId, toAccountId);

		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				transactionTemplate.executeWithoutResult(status -> executeTransfer(fromAccountId, toAccountId, amount));
				log.info("Transfer completed: from={} to={} amount={}", fromAccountId, toAccountId, amount);
				return;
			}
			catch (OptimisticLockingFailureException ex) {
				log.warn("Optimistic lock conflict on transfer attempt={}/{}", attempt, MAX_ATTEMPTS);
				if (attempt == MAX_ATTEMPTS) {
					throw ex;
				}
			}
		}
	}

	private void executeTransfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
		Account source = loadAccount(fromAccountId);
		Account destination = loadAccount(toAccountId);

		if (!source.hasSufficientFunds(amount)) {
			log.warn("Transfer rejected, insufficient funds: account={} amount={}", fromAccountId, amount);
			throw new InsufficientFundsException(fromAccountId);
		}

		source.debit(amount);
		destination.credit(amount);

		accountRepository.save(source);
		accountRepository.save(destination);
	}

	private void validateAmount(BigDecimal amount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidTransferAmountException();
		}
	}

	private void validateDifferentAccounts(UUID fromAccountId, UUID toAccountId) {
		if (fromAccountId.equals(toAccountId)) {
			throw new IllegalArgumentException("Source and destination accounts must differ");
		}
	}

	private Account loadAccount(UUID accountId) {
		return accountRepository.findById(accountId)
			.orElseThrow(() -> new AccountNotFoundException(accountId));
	}

}
