package com.aiconomy.tasks.service.exception;

import com.aiconomy.common.task.TaskStatus;

/**
 * Thrown when a task transition is not allowed in the current state.
 */
public class InvalidTaskStateException extends RuntimeException {

	public InvalidTaskStateException(TaskStatus expected, TaskStatus actual) {
		super("Invalid task state: expected " + expected + " but was " + actual);
	}

	public InvalidTaskStateException(String message) {
		super(message);
	}

}
