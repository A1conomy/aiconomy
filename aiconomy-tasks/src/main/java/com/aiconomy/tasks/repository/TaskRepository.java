package com.aiconomy.tasks.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiconomy.common.task.TaskStatus;
import com.aiconomy.tasks.domain.Task;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	List<Task> findByStatus(TaskStatus status);

}
