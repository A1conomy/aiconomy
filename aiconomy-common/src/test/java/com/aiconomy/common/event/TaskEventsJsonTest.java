package com.aiconomy.common.event;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test that all task/payment events round-trip through JSON.
 */
class TaskEventsJsonTest {

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.addModule(new JavaTimeModule())
			.build();

	@ParameterizedTest
	@MethodSource("events")
	void roundTripsThroughJson(Object event) throws Exception {
		String json = objectMapper.writeValueAsString(event);
		Object restored = objectMapper.readValue(json, event.getClass());
		assertThat(restored).isEqualTo(event);
	}

	static Stream<Object> events() {
		var instant = java.time.Instant.parse("2026-07-01T12:00:00Z");
		var id = java.util.UUID.fromString("11111111-1111-1111-1111-111111111111");
		var taskId = java.util.UUID.fromString("22222222-2222-2222-2222-222222222222");
		var accountId = java.util.UUID.fromString("33333333-3333-3333-3333-333333333333");
		var escrowId = java.util.UUID.fromString("44444444-4444-4444-4444-444444444444");
		var amount = new java.math.BigDecimal("150.00");

		return Stream.of(
				new TaskClaimedEvent(id, taskId, "worker-1", accountId, instant),
				new TaskDeliveredEvent(id, taskId, "worker-1", "Landing page deployed", instant),
				new TaskAcceptedEvent(id, taskId, "client-1", escrowId, instant),
				new TaskRejectedEvent(id, taskId, "client-1", escrowId, "Does not meet spec", instant),
				new PaymentProposedEvent(id, taskId, "manager-1", "client-1", amount, "Full stack delivery", instant),
				new PaymentAcceptedEvent(id, taskId, "client-1", "manager-1", amount, instant));
	}

}
