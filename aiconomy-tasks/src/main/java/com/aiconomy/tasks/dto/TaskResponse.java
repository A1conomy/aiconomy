package com.aiconomy.tasks.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.common.task.TaskSkill;
import com.aiconomy.common.task.TaskStatus;
import com.aiconomy.tasks.domain.Task;

/**
 * Task representation returned by the REST API.
 */
public record TaskResponse(

		UUID id,

		UUID projectId,

		String title,

		String description,

		TaskSkill requiredSkill,

		BigDecimal budget,

		String clientAgentId,

		UUID clientAccountId,

		TaskStatus status,

		String assigneeAgentId,

		UUID assigneeAccountId,

		UUID escrowHoldId,

		String deliverableNotes,

		Instant createdAt,

		Instant updatedAt

) {

	public static TaskResponse from(Task task) {
		return new TaskResponse(
				task.getId(),
				task.getProjectId(),
				task.getTitle(),
				task.getDescription(),
				task.getRequiredSkill(),
				task.getBudget(),
				task.getClientAgentId(),
				task.getClientAccountId(),
				task.getStatus(),
				task.getAssigneeAgentId(),
				task.getAssigneeAccountId(),
				task.getEscrowHoldId(),
				task.getDeliverableNotes(),
				task.getCreatedAt(),
				task.getUpdatedAt());
	}

}
