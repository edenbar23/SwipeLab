package com.swipelab.recipients.events;

import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientUserEventListener;
import com.swipelab.recipients.infrastructure.RecipientUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipientUserEventListenerTest {

    @Mock
    private RecipientUserRepository recipientUserRepository;

    @InjectMocks
    private RecipientUserEventListener recipientUserEventListener;

    @Test
    void handleUserCreated_ShouldCreateRecipientUser_WhenNotExists() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setUsername("testuser");

        when(recipientUserRepository.existsById("testuser")).thenReturn(false);

        recipientUserEventListener.handleUserCreated(event);

        verify(recipientUserRepository, times(1)).save(argThat(user -> 
                user.getUsername().equals("testuser") && user.isActive()
        ));
    }

    @Test
    void handleUserCreated_ShouldNotCreateRecipientUser_WhenExists() {
        UserCreatedEvent event = new UserCreatedEvent();
        event.setUsername("testuser");

        when(recipientUserRepository.existsById("testuser")).thenReturn(true);

        recipientUserEventListener.handleUserCreated(event);

        verify(recipientUserRepository, never()).save(any(RecipientUser.class));
    }

    @Test
    void handleUserStatusChanged_ShouldUpdateStatus_WhenExists() {
        UserStatusChangedEvent event = new UserStatusChangedEvent();
        event.setUsername("testuser");
        event.setActive(false);

        RecipientUser existingUser = new RecipientUser();
        existingUser.setUsername("testuser");
        existingUser.setActive(true);

        when(recipientUserRepository.findById("testuser")).thenReturn(Optional.of(existingUser));

        recipientUserEventListener.handleUserStatusChanged(event);

        verify(recipientUserRepository, times(1)).save(argThat(user -> 
                user.getUsername().equals("testuser") && !user.isActive()
        ));
    }

    @Test
    void handleUserStatusChanged_ShouldDoNothing_WhenNotExists() {
        UserStatusChangedEvent event = new UserStatusChangedEvent();
        event.setUsername("testuser");
        event.setActive(false);

        when(recipientUserRepository.findById("testuser")).thenReturn(Optional.empty());

        recipientUserEventListener.handleUserStatusChanged(event);

        verify(recipientUserRepository, never()).save(any(RecipientUser.class));
    }
}
