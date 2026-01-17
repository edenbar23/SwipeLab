package com.swipelab.tasks.infrastructure;

import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);

    List<Task> findByCreatedBy_Username(String username);
}
