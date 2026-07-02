package com.aiconomy.tasks.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for claiming an open task.
 */
public record ClaimTaskRequest(

		@NotBlank String agentId,

		@NotNull UUID agentAccountId

) {
}
