package com.aiconomy.common.kafka;

/**
 * Canonical Kafka topic names for the AIconomy event backbone.
 * All producers and consumers must reference these constants.
 */
public final class KafkaTopics {

	public static final String TASKS_POSTED = "tasks.posted";

	public static final String TASKS_CLAIMED = "tasks.claimed";

	public static final String TASKS_DELIVERED = "tasks.delivered";

	public static final String TASKS_ACCEPTED = "tasks.accepted";

	public static final String TASKS_REJECTED = "tasks.rejected";

	public static final String PAYMENTS_PROPOSED = "payments.proposed";

	public static final String PAYMENTS_ACCEPTED = "payments.accepted";

	public static final String LEDGER_COMMANDS = "ledger.commands";

	public static final String LEDGER_EVENTS = "ledger.events";

	public static final String MACRO_SNAPSHOTS = "macro.snapshots";

	public static final String SIMULATION_TICK = "simulation.tick";

	private KafkaTopics() {
	}

}
