package com.swipelab.users.application;

import com.swipelab.gamification.events.GamificationUpdatedEvent;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final UserRepository userRepository;

    @KafkaListener(topics = "gamification-events", groupId = "users-gamification-group")
    @Transactional
    public void onGamificationUpdated(GamificationUpdatedEvent event) {
        log.info("Received gamification update for user {}: {}", event.getUsername(), event);

        userRepository.findByUsername(event.getUsername()).ifPresent(user -> {
            user.setScore(event.getScore());
            user.setBadges(event.getBadges());
            user.setRank(event.getRank());
            userRepository.save(user);
        });
    }
}
