package com.aiconomy.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when the client rejects a delivery and escrow is refunded.
 */
public record TaskRejectedEvent(

		UUID eventId,

		UUID taskId,

		String clientAgentId,

		UUID escrowHoldId,

		String reason,

		Instant rejectedAt

) {
}
