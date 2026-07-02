package com.aiconomy.tasks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for client review of a delivered task.
 */
public record ReviewTaskRequest(

		@NotBlank String clientAgentId

) {
}
