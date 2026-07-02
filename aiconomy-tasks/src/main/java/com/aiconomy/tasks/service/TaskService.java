package com.aiconomy.tasks.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiconomy.common.task.TaskStatus;
import com.aiconomy.tasks.domain.Task;
import com.aiconomy.tasks.dto.ClaimTaskRequest;
import com.aiconomy.tasks.dto.DeliverTaskRequest;
import com.aiconomy.tasks.dto.PostTaskRequest;
import com.aiconomy.tasks.dto.RejectTaskRequest;
import com.aiconomy.tasks.dto.ReviewTaskRequest;
import com.aiconomy.tasks.event.TaskEventPublisher;
import com.aiconomy.tasks.repository.TaskRepository;
import com.aiconomy.tasks.service.exception.InvalidTaskStateException;
import com.aiconomy.tasks.service.exception.TaskAuthorizationException;
import com.aiconomy.tasks.service.exception.TaskNotFoundException;

/**
 * Manages task lifecycle transitions and escrow integration.
 */
@Service
public class TaskService {

	private static final Logger log = LoggerFactory.getLogger(TaskService.class);

	private final TaskRepository taskRepository;

	private final LedgerEscrowClient ledgerEscrowClient;

	private final TaskEventPublisher taskEventPublisher;

	public TaskService(
			TaskRepository taskRepository,
			LedgerEscrowClient ledgerEscrowClient,
			TaskEventPublisher taskEventPublisher) {
		this.taskRepository = taskRepository;
		this.ledgerEscrowClient = ledgerEscrowClient;
		this.taskEventPublisher = taskEventPublisher;
	}

	@Transactional
	public Task postTask(PostTaskRequest request) {
		Task task = new Task(
				UUID.randomUUID(),
				request.projectId(),
				request.title(),
				request.description(),
				request.requiredSkill(),
				request.budget(),
				request.clientAgentId(),
				request.clientAccountId());
		Task saved = taskRepository.save(task);
		log.info("Task posted: id={} skill={} budget={}", saved.getId(), saved.getRequiredSkill(), saved.getBudget());
		taskEventPublisher.publishPosted(saved);
		return saved;
	}

	@Transactional(readOnly = true)
	public Task getTask(UUID taskId) {
		return loadTask(taskId);
	}

	@Transactional(readOnly = true)
	public List<Task> listOpenTasks() {
		return taskRepository.findByStatus(TaskStatus.OPEN);
	}

	@Transactional
	public Task claimTask(UUID taskId, ClaimTaskRequest request) {
		Task task = loadTask(taskId);
		requireStatus(task, TaskStatus.OPEN);

		UUID escrowHoldId = ledgerEscrowClient.hold(
				task.getClientAccountId(),
				request.agentAccountId(),
				task.getId(),
				task.getBudget());
		task.claim(request.agentId(), request.agentAccountId(), escrowHoldId);

		Task saved = taskRepository.save(task);
		log.info("Task claimed: id={} assignee={}", saved.getId(), saved.getAssigneeAgentId());
		taskEventPublisher.publishClaimed(saved);
		return saved;
	}

	@Transactional
	public Task deliverTask(UUID taskId, DeliverTaskRequest request) {
		Task task = loadTask(taskId);
		requireStatus(task, TaskStatus.CLAIMED);
		requireAssignee(task, request.agentId());

		task.deliver(request.deliverableNotes());
		Task saved = taskRepository.save(task);
		log.info("Task delivered: id={}", saved.getId());
		taskEventPublisher.publishDelivered(saved);
		return saved;
	}

	@Transactional
	public Task acceptTask(UUID taskId, ReviewTaskRequest request) {
		Task task = loadTask(taskId);
		requireStatus(task, TaskStatus.DELIVERED);
		requireClient(task, request.clientAgentId());

		ledgerEscrowClient.release(task.getEscrowHoldId());
		task.accept();

		Task saved = taskRepository.save(task);
		log.info("Task accepted: id={}", saved.getId());
		taskEventPublisher.publishAccepted(saved);
		return saved;
	}

	@Transactional
	public Task rejectTask(UUID taskId, RejectTaskRequest request) {
		Task task = loadTask(taskId);
		requireStatus(task, TaskStatus.DELIVERED);
		requireClient(task, request.clientAgentId());

		ledgerEscrowClient.refund(task.getEscrowHoldId());
		task.reject();

		Task saved = taskRepository.save(task);
		log.info("Task rejected: id={}", saved.getId());
		taskEventPublisher.publishRejected(saved, request.reason());
		return saved;
	}

	private Task loadTask(UUID taskId) {
		return taskRepository.findById(taskId)
			.orElseThrow(() -> new TaskNotFoundException(taskId));
	}

	private void requireStatus(Task task, TaskStatus expected) {
		if (task.getStatus() != expected) {
			throw new InvalidTaskStateException(expected, task.getStatus());
		}
	}

	private void requireAssignee(Task task, String agentId) {
		if (!task.getAssigneeAgentId().equals(agentId)) {
			throw new TaskAuthorizationException("Only the assignee can deliver this task");
		}
	}

	private void requireClient(Task task, String clientAgentId) {
		if (!task.getClientAgentId().equals(clientAgentId)) {
			throw new TaskAuthorizationException("Only the client can review this task");
		}
	}

}
