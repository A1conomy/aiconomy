package com.aiconomy.tasks.event;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.aiconomy.common.event.TaskAcceptedEvent;
import com.aiconomy.common.event.TaskClaimedEvent;
import com.aiconomy.common.event.TaskDeliveredEvent;
import com.aiconomy.common.event.TaskPostedEvent;
import com.aiconomy.common.event.TaskRejectedEvent;
import com.aiconomy.common.kafka.KafkaTopics;
import com.aiconomy.tasks.domain.Task;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes task lifecycle events to Kafka for agent consumption.
 */
@Component
public class TaskEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(TaskEventPublisher.class);

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper;

	public TaskEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

	public void publishPosted(Task task) {
		publish(KafkaTopics.TASKS_POSTED, task.getId(), new TaskPostedEvent(
				UUID.randomUUID(),
				task.getId(),
				task.getProjectId(),
				task.getTitle(),
				task.getDescription(),
				task.getRequiredSkill(),
				task.getBudget(),
				task.getClientAgentId(),
				task.getClientAccountId(),
				Instant.now()));
	}

	public void publishClaimed(Task task) {
		publish(KafkaTopics.TASKS_CLAIMED, task.getId(), new TaskClaimedEvent(
				UUID.randomUUID(),
				task.getId(),
				task.getAssigneeAgentId(),
				task.getAssigneeAccountId(),
				Instant.now()));
	}

	public void publishDelivered(Task task) {
		publish(KafkaTopics.TASKS_DELIVERED, task.getId(), new TaskDeliveredEvent(
				UUID.randomUUID(),
				task.getId(),
				task.getAssigneeAgentId(),
				task.getDeliverableNotes(),
				Instant.now()));
	}

	public void publishAccepted(Task task) {
		publish(KafkaTopics.TASKS_ACCEPTED, task.getId(), new TaskAcceptedEvent(
				UUID.randomUUID(),
				task.getId(),
				task.getClientAgentId(),
				task.getEscrowHoldId(),
				Instant.now()));
	}

	public void publishRejected(Task task, String reason) {
		publish(KafkaTopics.TASKS_REJECTED, task.getId(), new TaskRejectedEvent(
				UUID.randomUUID(),
				task.getId(),
				task.getClientAgentId(),
				task.getEscrowHoldId(),
				reason,
				Instant.now()));
	}

	private void publish(String topic, UUID key, Object event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, key.toString(), payload);
			log.info("Published {} for taskId={}", topic, key);
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Failed to serialize task event for topic " + topic, ex);
		}
	}

}
