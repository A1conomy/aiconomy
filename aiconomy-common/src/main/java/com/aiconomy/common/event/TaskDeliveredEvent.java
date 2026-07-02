package com.aiconomy.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when the assignee submits work for client review.
 */
public record TaskDeliveredEvent(

		UUID eventId,

		UUID taskId,

		String agentId,

		String deliverableNotes,

		Instant deliveredAt

) {
}
