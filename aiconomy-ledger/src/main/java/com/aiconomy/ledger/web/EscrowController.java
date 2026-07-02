package com.aiconomy.ledger.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aiconomy.ledger.dto.EscrowResponse;
import com.aiconomy.ledger.dto.HoldEscrowRequest;
import com.aiconomy.ledger.service.EscrowService;

import jakarta.validation.Valid;

/**
 * REST API for escrow hold, release, and refund operations.
 */
@RestController
@RequestMapping("/api/v1/escrow")
public class EscrowController {

	private final EscrowService escrowService;

	public EscrowController(EscrowService escrowService) {
		this.escrowService = escrowService;
	}

	@PostMapping("/hold")
	@ResponseStatus(HttpStatus.CREATED)
	public EscrowResponse hold(@Valid @RequestBody HoldEscrowRequest request) {
		return escrowService.hold(request);
	}

	@PostMapping("/{escrowHoldId}/release")
	public EscrowResponse release(@PathVariable UUID escrowHoldId) {
		return escrowService.release(escrowHoldId);
	}

	@PostMapping("/{escrowHoldId}/refund")
	public EscrowResponse refund(@PathVariable UUID escrowHoldId) {
		return escrowService.refund(escrowHoldId);
	}

}
