package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.TaskDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TaskDailyStatsRepository extends JpaRepository<TaskDailyStats, Long> {
    Optional<TaskDailyStats> findByTaskIdAndDay(Long taskId, LocalDate day);
}
