package com.swipelab.tasks.infrastructure;

import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);

    long countByStatus(TaskStatus status);

    List<Task> findByCreatedBy_Username(String username);

    org.springframework.data.domain.Page<Task> findByStatusAndRecipientGroupsIn(
            TaskStatus status,
            java.util.Collection<Long> recipientGroups,
            org.springframework.data.domain.Pageable pageable);

}
