package com.aiconomy.common.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsTest {

	@Test
	void taskAndLedgerTopicsAreStableStrings() {
		assertThat(KafkaTopics.TASKS_POSTED).isEqualTo("tasks.posted");
		assertThat(KafkaTopics.TASKS_CLAIMED).isEqualTo("tasks.claimed");
		assertThat(KafkaTopics.PAYMENTS_PROPOSED).isEqualTo("payments.proposed");
		assertThat(KafkaTopics.LEDGER_COMMANDS).isEqualTo("ledger.commands");
		assertThat(KafkaTopics.SIMULATION_TICK).isEqualTo("simulation.tick");
	}

}
