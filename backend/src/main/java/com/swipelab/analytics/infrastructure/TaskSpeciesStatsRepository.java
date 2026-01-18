package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.TaskSpeciesStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskSpeciesStatsRepository extends JpaRepository<TaskSpeciesStats, Long> {
    Optional<TaskSpeciesStats> findByTaskIdAndSpecies(Long taskId, String species);

    List<TaskSpeciesStats> findByTaskId(Long taskId);
}
