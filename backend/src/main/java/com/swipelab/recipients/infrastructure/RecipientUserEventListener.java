package com.swipelab.recipients.infrastructure;

import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.events.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipientUserEventListener {

    private final RecipientUserRepository recipientUserRepository;

    @KafkaListener(topics = "user-events", groupId = "recipient-group-service-created")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("Received UserCreatedEvent for user: {}", event.getUsername());

        if (!recipientUserRepository.existsById(event.getUsername())) {
            RecipientUser recipientUser = RecipientUser.builder()
                    .username(event.getUsername())
                    .active(true)
                    .build();
            recipientUserRepository.save(recipientUser);
            log.info("Created RecipientUser for: {}", event.getUsername());
        } else {
            log.info("RecipientUser already exists: {}", event.getUsername());
        }
    }

    @KafkaListener(topics = "user-events", groupId = "recipient-group-service-status")
    public void handleUserStatusChanged(com.swipelab.recipients.events.UserStatusChangedEvent event) {
        log.info("Received UserStatusChangedEvent for user: {}", event.getUsername());
        recipientUserRepository.findById(event.getUsername()).ifPresent(user -> {
            user.setActive(event.isActive());
            recipientUserRepository.save(user);
            log.info("Updated status for user: {} to active={}", event.getUsername(), event.isActive());
        });
    }
}
