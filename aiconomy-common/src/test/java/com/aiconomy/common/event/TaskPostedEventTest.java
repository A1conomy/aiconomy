package com.aiconomy.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.aiconomy.common.task.TaskSkill;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies TaskPostedEvent serializes to stable JSON field names for Python agent interop.
 */
class TaskPostedEventTest {

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.addModule(new JavaTimeModule())
			.build();

	@Test
	void roundTripsThroughJsonWithExpectedFieldNames() throws Exception {
		Instant postedAt = Instant.parse("2026-07-01T12:00:00Z");
		TaskPostedEvent original = new TaskPostedEvent(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				UUID.fromString("33333333-3333-3333-3333-333333333333"),
				"Build landing page",
				"Responsive page for product launch",
				TaskSkill.FRONTEND,
				new BigDecimal("500.00"),
				"client-agent-1",
				UUID.fromString("44444444-4444-4444-4444-444444444444"),
				postedAt);

		String json = objectMapper.writeValueAsString(original);

		assertThat(json).contains("\"eventId\":\"11111111-1111-1111-1111-111111111111\"");
		assertThat(json).contains("\"taskId\":\"22222222-2222-2222-2222-222222222222\"");
		assertThat(json).contains("\"requiredSkill\":\"FRONTEND\"");
		assertThat(json).contains("\"budget\":500.00");
		assertThat(json).contains("\"clientAgentId\":\"client-agent-1\"");

		TaskPostedEvent restored = objectMapper.readValue(json, TaskPostedEvent.class);

		assertThat(restored).isEqualTo(original);
	}

}
