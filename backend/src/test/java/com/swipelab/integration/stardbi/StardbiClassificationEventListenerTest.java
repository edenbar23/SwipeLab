package com.swipelab.integration.stardbi;

import com.swipelab.classification.domain.Image;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.integration.stardbi.dto.ExternalLabelDto;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    StardbiClassificationEventListenerTest.RetryConfig.class,
    StardbiClassificationEventListener.class
})
class StardbiClassificationEventListenerTest {

    @TestConfiguration
    @EnableRetry
    static class RetryConfig {
    }

    @MockBean
    private StardbiClientPort stardbiClient;

    @MockBean
    private TaskRepository taskRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ImageRepository imageRepository;

    @Autowired
    private StardbiClassificationEventListener listener;

    private ClassificationSubmittedEvent event;
    private Task task;
    private User user;
    private Image image;

    @BeforeEach
    void setUp() {
        event = ClassificationSubmittedEvent.builder()
                .taskId(100L)
                .username("test_researcher")
                .imageId(200L)
                .isCorrect(true)
                .species("Lion")
                .build();

        task = new Task();
        task.setSourceSystem("STARDBI");

        user = new User();
        user.setUsername("test_researcher");

        image = new Image();
        image.setId(200L);
        image.setParentImageId(999L);
    }

    @Test
    void testNormalExecution_successMapping() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("test_researcher")).thenReturn(Optional.of(user));
        when(imageRepository.findById(200L)).thenReturn(Optional.of(image));
        when(stardbiClient.getTaxonomy()).thenReturn(Collections.emptyList());

        listener.handleClassificationEvent(event);

        verify(stardbiClient, times(1)).postLabel(any(ExternalLabelDto.class));
    }

    @Test
    void testRetryLogic_shouldRetryThreeTimesThenRecover() {
        when(taskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(userRepository.findByUsername("test_researcher")).thenReturn(Optional.of(user));
        when(imageRepository.findById(200L)).thenReturn(Optional.of(image));
        when(stardbiClient.getTaxonomy()).thenReturn(Collections.emptyList());

        // Simulate 500 Internal Server error which triggers standard exception retry
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Down"))
                .when(stardbiClient).postLabel(any(ExternalLabelDto.class));

        // Call the method - it shouldn't throw an exception because it recovers
        listener.handleClassificationEvent(event);

        // Based on maxAttempts = 3, it should try exactly 3 times
        verify(stardbiClient, times(3)).postLabel(any(ExternalLabelDto.class));
    }
}
