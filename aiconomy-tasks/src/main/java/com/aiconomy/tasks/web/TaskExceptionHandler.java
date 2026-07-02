package com.aiconomy.tasks.web;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.aiconomy.tasks.service.exception.InvalidTaskStateException;
import com.aiconomy.tasks.service.exception.TaskAuthorizationException;
import com.aiconomy.tasks.service.exception.TaskNotFoundException;

/**
 * Maps task domain exceptions to HTTP status codes.
 */
@RestControllerAdvice
public class TaskExceptionHandler {

	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(TaskNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidTaskStateException.class)
	public ResponseEntity<Map<String, String>> handleInvalidState(InvalidTaskStateException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(TaskAuthorizationException.class)
	public ResponseEntity<Map<String, String>> handleAuthorization(TaskAuthorizationException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("Validation failed");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleMalformedJson(HttpMessageNotReadableException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed JSON request body"));
	}

}
