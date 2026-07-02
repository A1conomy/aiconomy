package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.aiconomy.common.task.TaskSkill;

/**
 * Published when a client (or manager on their behalf) posts a new task to the board.
 * Consumed by worker/manager agents and the task service.
 */
public record TaskPostedEvent(

		UUID eventId,

		UUID taskId,

		UUID projectId,

		String title,

		String description,

		TaskSkill requiredSkill,

		BigDecimal budget,

		String clientAgentId,

		UUID clientAccountId,

		Instant postedAt

) {
}
