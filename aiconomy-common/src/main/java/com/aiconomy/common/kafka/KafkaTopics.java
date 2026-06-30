package com.aiconomy.common.kafka;

/**
 * Canonical Kafka topic names for the AIconomy event backbone.
 * All producers and consumers must reference these constants.
 */
public final class KafkaTopics {

	public static final String ORDERS_SUBMITTED = "orders.submitted";

	public static final String TRADES_EXECUTED = "trades.executed";

	public static final String LEDGER_COMMANDS = "ledger.commands";

	public static final String LEDGER_EVENTS = "ledger.events";

	public static final String MARKET_QUOTES = "market.quotes";

	public static final String MACRO_SNAPSHOTS = "macro.snapshots";

	public static final String SIMULATION_TICK = "simulation.tick";

	private KafkaTopics() {
	}

}
