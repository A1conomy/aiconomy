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
 * Locks client funds for a task until release to worker or refund to client.
 */
@Entity
@Table(name = "escrow_holds")
public class EscrowHold {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "from_account_id", nullable = false, updatable = false)
	private UUID fromAccountId;

	@Column(name = "to_account_id", nullable = false, updatable = false)
	private UUID toAccountId;

	@Column(name = "task_id", nullable = false, updatable = false)
	private UUID taskId;

	@Column(nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EscrowStatus status;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected EscrowHold() {
	}

	public EscrowHold(UUID id, UUID fromAccountId, UUID toAccountId, UUID taskId, BigDecimal amount) {
		this.id = id;
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.taskId = taskId;
		this.amount = amount;
		this.status = EscrowStatus.ACTIVE;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getFromAccountId() {
		return fromAccountId;
	}

	public UUID getToAccountId() {
		return toAccountId;
	}

	public UUID getTaskId() {
		return taskId;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public EscrowStatus getStatus() {
		return status;
	}

	public void markReleased() {
		this.status = EscrowStatus.RELEASED;
		this.updatedAt = Instant.now();
	}

	public void markRefunded() {
		this.status = EscrowStatus.REFUNDED;
		this.updatedAt = Instant.now();
	}

	public boolean isActive() {
		return status == EscrowStatus.ACTIVE;
	}

}
