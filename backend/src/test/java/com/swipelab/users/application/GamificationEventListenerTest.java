package com.swipelab.users.application;

import com.swipelab.gamification.events.GamificationUpdatedEvent;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamificationEventListener gamificationEventListener;

    @Test
    void onGamificationUpdated_ShouldUpdateUser_WhenExists() {
        GamificationUpdatedEvent event = new GamificationUpdatedEvent(
                "testuser", 500L, "Gold Badge", "Master"
        );

        User user = new User();
        user.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        gamificationEventListener.onGamificationUpdated(event);

        verify(userRepository, times(1)).save(argThat(u -> 
                u.getScore() == 500L && 
                "Gold Badge".equals(u.getBadges()) && 
                "Master".equals(u.getRank())
        ));
    }

    @Test
    void onGamificationUpdated_ShouldDoNothing_WhenUserNotExists() {
        GamificationUpdatedEvent event = new GamificationUpdatedEvent(
                "testuser", 500L, "Gold Badge", "Master"
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        gamificationEventListener.onGamificationUpdated(event);

        verify(userRepository, never()).save(any(User.class));
    }
}
