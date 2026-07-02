package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a payment proposal is accepted by the counterparty.
 */
public record PaymentAcceptedEvent(

		UUID eventId,

		UUID taskId,

		String fromAgentId,

		String toAgentId,

		BigDecimal amount,

		Instant acceptedAt

) {
}
