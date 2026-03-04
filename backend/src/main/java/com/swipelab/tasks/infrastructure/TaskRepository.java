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

    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT t FROM Task t " +
        "LEFT JOIN t.recipientGroups rg " +
        "LEFT JOIN t.assignedUsernames au " +
        "WHERE t.status = :status AND " +
        "(t.isPublic = true OR au = :username OR rg IN :groupIds)"
    )
    org.springframework.data.domain.Page<Task> findAccessibleTasksForUser(
            @org.springframework.data.repository.query.Param("status") TaskStatus status,
            @org.springframework.data.repository.query.Param("username") String username,
            @org.springframework.data.repository.query.Param("groupIds") java.util.Collection<Long> groupIds,
            org.springframework.data.domain.Pageable pageable);

}
