package com.aiconomy.tasks.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.common.task.TaskSkill;
import com.aiconomy.common.task.TaskStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A unit of work posted on the task board.
 */
@Entity
@Table(name = "tasks")
public class Task {

	@Id
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private UUID projectId;

	@Column(nullable = false, updatable = false)
	private String title;

	@Column(nullable = false, updatable = false, columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "required_skill", nullable = false, updatable = false)
	private TaskSkill requiredSkill;

	@Column(nullable = false, updatable = false, precision = 19, scale = 2)
	private BigDecimal budget;

	@Column(name = "client_agent_id", nullable = false, updatable = false)
	private String clientAgentId;

	@Column(name = "client_account_id", nullable = false, updatable = false)
	private UUID clientAccountId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TaskStatus status;

	@Column(name = "assignee_agent_id")
	private String assigneeAgentId;

	@Column(name = "assignee_account_id")
	private UUID assigneeAccountId;

	@Column(name = "escrow_hold_id")
	private UUID escrowHoldId;

	@Column(name = "deliverable_notes", columnDefinition = "TEXT")
	private String deliverableNotes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Task() {
	}

	public Task(
			UUID id,
			UUID projectId,
			String title,
			String description,
			TaskSkill requiredSkill,
			BigDecimal budget,
			String clientAgentId,
			UUID clientAccountId) {
		this.id = id;
		this.projectId = projectId;
		this.title = title;
		this.description = description;
		this.requiredSkill = requiredSkill;
		this.budget = budget;
		this.clientAgentId = clientAgentId;
		this.clientAccountId = clientAccountId;
		this.status = TaskStatus.OPEN;
		this.createdAt = Instant.now();
		this.updatedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public UUID getProjectId() {
		return projectId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public TaskSkill getRequiredSkill() {
		return requiredSkill;
	}

	public BigDecimal getBudget() {
		return budget;
	}

	public String getClientAgentId() {
		return clientAgentId;
	}

	public UUID getClientAccountId() {
		return clientAccountId;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getAssigneeAgentId() {
		return assigneeAgentId;
	}

	public UUID getAssigneeAccountId() {
		return assigneeAccountId;
	}

	public UUID getEscrowHoldId() {
		return escrowHoldId;
	}

	public String getDeliverableNotes() {
		return deliverableNotes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void claim(String agentId, UUID agentAccountId, UUID escrowHoldId) {
		this.assigneeAgentId = agentId;
		this.assigneeAccountId = agentAccountId;
		this.escrowHoldId = escrowHoldId;
		this.status = TaskStatus.CLAIMED;
		this.updatedAt = Instant.now();
	}

	public void deliver(String deliverableNotes) {
		this.deliverableNotes = deliverableNotes;
		this.status = TaskStatus.DELIVERED;
		this.updatedAt = Instant.now();
	}

	public void accept() {
		this.status = TaskStatus.ACCEPTED;
		this.updatedAt = Instant.now();
	}

	public void reject() {
		this.status = TaskStatus.REJECTED;
		this.updatedAt = Instant.now();
	}

}
