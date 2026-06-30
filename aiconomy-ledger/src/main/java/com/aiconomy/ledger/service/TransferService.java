package com.aiconomy.ledger.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.service.exception.AccountNotFoundException;
import com.aiconomy.ledger.service.exception.InsufficientFundsException;
import com.aiconomy.ledger.service.exception.InvalidTransferAmountException;

/**
 * Executes ACID transfers between two accounts.
 * All balance changes happen inside a single database transaction.
 */
@Service
public class TransferService {

	private static final Logger log = LoggerFactory.getLogger(TransferService.class);

	private final AccountRepository accountRepository;

	public TransferService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	/**
	 * Moves funds from source to destination atomically.
	 * Rolls back entirely if any step fails.
	 */
	@Transactional
	public void transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount) {
		log.info("Transfer started: from={} to={} amount={}", fromAccountId, toAccountId, amount);

		validateAmount(amount);
		validateDifferentAccounts(fromAccountId, toAccountId);

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

		log.info("Transfer completed: from={} to={} amount={}", fromAccountId, toAccountId, amount);
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
