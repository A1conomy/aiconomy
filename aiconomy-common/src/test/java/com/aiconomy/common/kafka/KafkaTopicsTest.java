package com.aiconomy.common.kafka;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaTopicsTest {

	@Test
	void topicsAreStableStrings() {
		assertThat(KafkaTopics.ORDERS_SUBMITTED).isEqualTo("orders.submitted");
		assertThat(KafkaTopics.TRADES_EXECUTED).isEqualTo("trades.executed");
		assertThat(KafkaTopics.LEDGER_COMMANDS).isEqualTo("ledger.commands");
	}

}
