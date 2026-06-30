package com.aiconomy.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A bank account in the ledger. Balance is stored as BigDecimal (never double).
 * The version field enables optimistic locking for concurrent transfers.
 */
@Entity
@Table(name = "accounts")
public class Account {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "owner_id", nullable = false, updatable = false)
	private String ownerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "account_type", nullable = false, updatable = false)
	private AccountType accountType;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Account() {
		// Required by JPA
	}

	public Account(UUID id, String ownerId, AccountType accountType, BigDecimal balance) {
		this.id = id;
		this.ownerId = ownerId;
		this.accountType = accountType;
		this.balance = balance;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public AccountType getAccountType() {
		return accountType;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public Long getVersion() {
		return version;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	/**
	 * Returns true if balance is greater than or equal to the requested amount.
	 */
	public boolean hasSufficientFunds(BigDecimal amount) {
		return balance.compareTo(amount) >= 0;
	}

	/**
	 * Subtracts amount from balance. Caller must verify sufficient funds first.
	 */
	public void debit(BigDecimal amount) {
		this.balance = this.balance.subtract(amount);
		this.updatedAt = Instant.now();
	}

	/**
	 * Adds amount to balance.
	 */
	public void credit(BigDecimal amount) {
		this.balance = this.balance.add(amount);
		this.updatedAt = Instant.now();
	}

}
