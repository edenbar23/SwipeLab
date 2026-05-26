package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.SuspiciousActivityRecord;
import com.swipelab.classification.domain.WarningLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivityRecord, Long> {

    /** All records for a user, newest first — used for the admin audit log view. */
    List<SuspiciousActivityRecord> findByUsernameOrderByCreatedAtDesc(String username);

    /**
     * Count suspicious raw events for a user within a sliding time window.
     * Used by the fraud detection engine to decide whether to record a STRIKE.
     */
    @Query("""
            SELECT COUNT(s) FROM SuspiciousActivityRecord s
            WHERE s.username = :username
              AND s.severity = :severity
              AND s.createdAt >= :since
            """)
    long countByUsernameAndSeverityAndCreatedAtAfter(
            @Param("username") String username,
            @Param("severity") WarningLevel severity,
            @Param("since") LocalDateTime since);

    /** All records for a user at or above the given severity — for audit. */
    List<SuspiciousActivityRecord> findByUsernameAndSeverityOrderByCreatedAtDesc(
            String username, WarningLevel severity);
}
