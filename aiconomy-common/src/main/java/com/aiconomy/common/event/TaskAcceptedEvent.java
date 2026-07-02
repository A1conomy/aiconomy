package com.aiconomy.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when the client accepts a delivered task and escrow is released.
 */
public record TaskAcceptedEvent(

		UUID eventId,

		UUID taskId,

		String clientAgentId,

		UUID escrowHoldId,

		Instant acceptedAt

) {
}
