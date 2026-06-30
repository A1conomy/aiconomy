package com.aiconomy.ledger.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aiconomy.ledger.dto.AccountResponse;
import com.aiconomy.ledger.dto.CreateAccountRequest;
import com.aiconomy.ledger.dto.TransferRequest;
import com.aiconomy.ledger.service.AccountService;
import com.aiconomy.ledger.service.TransferService;

import jakarta.validation.Valid;

/**
 * REST API for the Core Banking Ledger.
 * Controllers stay thin — all business rules live in services.
 */
@RestController
@RequestMapping("/api/v1")
public class AccountController {

	private final AccountService accountService;

	private final TransferService transferService;

	public AccountController(AccountService accountService, TransferService transferService) {
		this.accountService = accountService;
		this.transferService = transferService;
	}

	@PostMapping("/accounts")
	@ResponseStatus(HttpStatus.CREATED)
	public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
		return AccountResponse.from(accountService.createAccount(request));
	}

	@GetMapping("/accounts/{accountId}")
	public AccountResponse getAccount(@PathVariable UUID accountId) {
		return AccountResponse.from(accountService.getAccount(accountId));
	}

	@PostMapping("/transfers")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void transfer(@Valid @RequestBody TransferRequest request) {
		transferService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
	}

}
