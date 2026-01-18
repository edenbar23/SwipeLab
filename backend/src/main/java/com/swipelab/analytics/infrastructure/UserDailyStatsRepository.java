package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.UserDailyStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyStatsRepository extends JpaRepository<UserDailyStats, Long> {

    Optional<UserDailyStats> findByUserIdAndDay(String userId, LocalDate day);

    @Query("SELECT SUM(u.total) as total, SUM(u.correct) as correct " +
            "FROM UserDailyStats u WHERE u.userId = :userId " +
            "AND u.day >= :startDate")
    Object getProgressSince(@Param("userId") String userId, @Param("startDate") LocalDate startDate);

    @Query("SELECT SUM(u.total) as total, SUM(u.correct) as correct " +
            "FROM UserDailyStats u WHERE u.userId = :userId")
    Object getTotalStats(@Param("userId") String userId);

    List<UserDailyStats> findByUserIdAndDayAfterOrderByDayAsc(String userId, LocalDate after);
}
