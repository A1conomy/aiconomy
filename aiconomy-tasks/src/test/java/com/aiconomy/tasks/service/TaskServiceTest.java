package com.aiconomy.tasks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aiconomy.common.task.TaskSkill;
import com.aiconomy.common.task.TaskStatus;
import com.aiconomy.tasks.domain.Task;
import com.aiconomy.tasks.dto.ClaimTaskRequest;
import com.aiconomy.tasks.dto.DeliverTaskRequest;
import com.aiconomy.tasks.dto.PostTaskRequest;
import com.aiconomy.tasks.dto.ReviewTaskRequest;
import com.aiconomy.tasks.event.TaskEventPublisher;
import com.aiconomy.tasks.repository.TaskRepository;
import com.aiconomy.tasks.service.exception.InvalidTaskStateException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	private TaskRepository taskRepository;

	@Mock
	private LedgerEscrowClient ledgerEscrowClient;

	@Mock
	private TaskEventPublisher taskEventPublisher;

	private TaskService taskService;

	private UUID taskId;

	private UUID projectId;

	private UUID clientAccountId;

	private UUID workerAccountId;

	private UUID escrowId;

	@BeforeEach
	void setUp() {
		taskService = new TaskService(taskRepository, ledgerEscrowClient, taskEventPublisher);
		taskId = UUID.randomUUID();
		projectId = UUID.randomUUID();
		clientAccountId = UUID.randomUUID();
		workerAccountId = UUID.randomUUID();
		escrowId = UUID.randomUUID();
	}

	@Test
	void postTaskCreatesOpenTaskAndPublishesEvent() {
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task task = taskService.postTask(new PostTaskRequest(
				projectId,
				"Landing page",
				"Build responsive landing page",
				TaskSkill.FRONTEND,
				new BigDecimal("400.00"),
				"client-1",
				clientAccountId));

		assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
		verify(taskEventPublisher).publishPosted(task);
	}

	@Test
	void claimTaskHoldsEscrowAndMovesToClaimed() {
		Task openTask = sampleTask(TaskStatus.OPEN);
		when(taskRepository.findById(taskId)).thenReturn(Optional.of(openTask));
		when(ledgerEscrowClient.hold(clientAccountId, workerAccountId, taskId, openTask.getBudget())).thenReturn(escrowId);
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task claimed = taskService.claimTask(taskId, new ClaimTaskRequest("worker-1", workerAccountId));

		assertThat(claimed.getStatus()).isEqualTo(TaskStatus.CLAIMED);
		assertThat(claimed.getEscrowHoldId()).isEqualTo(escrowId);
		verify(taskEventPublisher).publishClaimed(claimed);
	}

	@Test
	void deliverTaskRequiresClaimedState() {
		Task openTask = sampleTask(TaskStatus.OPEN);
		when(taskRepository.findById(taskId)).thenReturn(Optional.of(openTask));

		assertThatThrownBy(() -> taskService.deliverTask(taskId, new DeliverTaskRequest("worker-1", "Done")))
			.isInstanceOf(InvalidTaskStateException.class);
	}

	@Test
	void acceptTaskReleasesEscrow() {
		Task deliveredTask = claimedTask();
		deliveredTask.deliver("Deployed to staging");
		when(taskRepository.findById(taskId)).thenReturn(Optional.of(deliveredTask));
		when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Task accepted = taskService.acceptTask(taskId, new ReviewTaskRequest("client-1"));

		assertThat(accepted.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
		verify(ledgerEscrowClient).release(escrowId);
		verify(taskEventPublisher).publishAccepted(accepted);
	}

	private Task sampleTask(TaskStatus status) {
		Task task = new Task(
				taskId,
				projectId,
				"Landing page",
				"Build responsive landing page",
				TaskSkill.FRONTEND,
				new BigDecimal("400.00"),
				"client-1",
				clientAccountId);
		if (status != TaskStatus.OPEN) {
			task.claim("worker-1", workerAccountId, escrowId);
		}
		return task;
	}

	private Task claimedTask() {
		return sampleTask(TaskStatus.CLAIMED);
	}

}
