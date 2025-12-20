package com.swipelab.repository;

import com.swipelab.model.entity.Task;
import com.swipelab.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);

    List<Task> findByCreatedBy_Username(String username);
}
