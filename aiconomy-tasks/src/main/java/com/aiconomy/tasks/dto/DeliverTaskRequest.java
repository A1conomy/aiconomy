package com.aiconomy.tasks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for submitting a task delivery.
 */
public record DeliverTaskRequest(

		@NotBlank String agentId,

		@NotBlank String deliverableNotes

) {
}
