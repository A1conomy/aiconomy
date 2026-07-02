package com.aiconomy.tasks.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aiconomy.tasks.dto.ClaimTaskRequest;
import com.aiconomy.tasks.dto.DeliverTaskRequest;
import com.aiconomy.tasks.dto.PostTaskRequest;
import com.aiconomy.tasks.dto.RejectTaskRequest;
import com.aiconomy.tasks.dto.ReviewTaskRequest;
import com.aiconomy.tasks.dto.TaskResponse;
import com.aiconomy.tasks.service.TaskService;

import jakarta.validation.Valid;

/**
 * REST API for the task marketplace board.
 */
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TaskResponse postTask(@Valid @RequestBody PostTaskRequest request) {
		return TaskResponse.from(taskService.postTask(request));
	}

	@GetMapping("/{taskId}")
	public TaskResponse getTask(@PathVariable UUID taskId) {
		return TaskResponse.from(taskService.getTask(taskId));
	}

	@GetMapping
	public List<TaskResponse> listOpenTasks() {
		return taskService.listOpenTasks().stream()
			.map(TaskResponse::from)
			.toList();
	}

	@PostMapping("/{taskId}/claim")
	public TaskResponse claimTask(@PathVariable UUID taskId, @Valid @RequestBody ClaimTaskRequest request) {
		return TaskResponse.from(taskService.claimTask(taskId, request));
	}

	@PostMapping("/{taskId}/deliver")
	public TaskResponse deliverTask(@PathVariable UUID taskId, @Valid @RequestBody DeliverTaskRequest request) {
		return TaskResponse.from(taskService.deliverTask(taskId, request));
	}

	@PostMapping("/{taskId}/accept")
	public TaskResponse acceptTask(@PathVariable UUID taskId, @Valid @RequestBody ReviewTaskRequest request) {
		return TaskResponse.from(taskService.acceptTask(taskId, request));
	}

	@PostMapping("/{taskId}/reject")
	public TaskResponse rejectTask(@PathVariable UUID taskId, @Valid @RequestBody RejectTaskRequest request) {
		return TaskResponse.from(taskService.rejectTask(taskId, request));
	}

}
