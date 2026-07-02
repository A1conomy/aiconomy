package com.aiconomy.common.task;

/**
 * Lifecycle states for a task on the marketplace board.
 * Transitions: OPEN → CLAIMED → DELIVERED → ACCEPTED | REJECTED
 */
public enum TaskStatus {

	OPEN,
	CLAIMED,
	DELIVERED,
	ACCEPTED,
	REJECTED

}
