package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.UserRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRankingRepository extends JpaRepository<UserRanking, Long> {
    Optional<UserRanking> findByUserIdAndPeriod(String userId, String period);
}
