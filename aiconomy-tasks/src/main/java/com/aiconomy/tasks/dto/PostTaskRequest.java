package com.aiconomy.tasks.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.aiconomy.common.task.TaskSkill;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for posting a new task to the board.
 */
public record PostTaskRequest(

		@NotNull UUID projectId,

		@NotBlank String title,

		@NotBlank String description,

		@NotNull TaskSkill requiredSkill,

		@NotNull @DecimalMin(value = "0.01") BigDecimal budget,

		@NotBlank String clientAgentId,

		@NotNull UUID clientAccountId

) {
}
