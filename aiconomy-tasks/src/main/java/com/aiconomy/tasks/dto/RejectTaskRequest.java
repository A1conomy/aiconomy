package com.aiconomy.tasks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for rejecting a delivered task.
 */
public record RejectTaskRequest(

		@NotBlank String clientAgentId,

		@NotBlank String reason

) {
}
