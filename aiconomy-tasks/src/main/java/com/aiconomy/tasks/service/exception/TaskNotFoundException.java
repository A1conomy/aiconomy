package com.aiconomy.tasks.service.exception;

import java.util.UUID;

/**
 * Thrown when a task cannot be found.
 */
public class TaskNotFoundException extends RuntimeException {

	public TaskNotFoundException(UUID taskId) {
		super("Task not found: " + taskId);
	}

}
