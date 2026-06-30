package com.aiconomy.ledger.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.dto.CreateAccountRequest;
import com.aiconomy.ledger.repository.AccountRepository;
import com.aiconomy.ledger.service.exception.AccountNotFoundException;

/**
 * Creates and retrieves ledger accounts.
 */
@Service
public class AccountService {

	private static final Logger log = LoggerFactory.getLogger(AccountService.class);

	private final AccountRepository accountRepository;

	public AccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Transactional
	public Account createAccount(CreateAccountRequest request) {
		Account account = new Account(
				UUID.randomUUID(),
				request.ownerId(),
				request.accountType(),
				request.initialBalance());

		Account saved = accountRepository.save(account);
		log.info("Account created: id={} owner={} type={} balance={}",
				saved.getId(), saved.getOwnerId(), saved.getAccountType(), saved.getBalance());
		return saved;
	}

	@Transactional(readOnly = true)
	public Account getAccount(UUID accountId) {
		return accountRepository.findById(accountId)
			.orElseThrow(() -> new AccountNotFoundException(accountId));
	}

}
