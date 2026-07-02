package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when an agent proposes payment for a task or subcontract.
 */
public record PaymentProposedEvent(

		UUID eventId,

		UUID taskId,

		String fromAgentId,

		String toAgentId,

		BigDecimal amount,

		String rationale,

		Instant proposedAt

) {
}
