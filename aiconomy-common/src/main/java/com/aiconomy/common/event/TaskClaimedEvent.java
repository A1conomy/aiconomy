package com.aiconomy.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a worker or manager claims an open task.
 */
public record TaskClaimedEvent(

		UUID eventId,

		UUID taskId,

		String agentId,

		UUID agentAccountId,

		Instant claimedAt

) {
}
