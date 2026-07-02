package com.aiconomy.tasks.service.exception;

/**
 * Thrown when the caller is not authorized to act on a task.
 */
public class TaskAuthorizationException extends RuntimeException {

	public TaskAuthorizationException(String message) {
		super(message);
	}

}
