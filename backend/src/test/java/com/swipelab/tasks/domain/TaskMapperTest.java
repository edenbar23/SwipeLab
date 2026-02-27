package com.swipelab.tasks.domain;

import com.swipelab.classification.domain.Label;
import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private TaskMapper taskMapper;

    @BeforeEach
    void setUp() {
        taskMapper = new TaskMapper();
    }

    @Test
    void toEntity_ShouldMapCreateRequestToTask() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("My Task");
        request.setDescription("Desc");
        request.setMinClassificationsPerImage(5);
        request.setConsensusThreshold(0.8);
        request.setRecipientGroups(Collections.singletonList(1L));

        Task task = taskMapper.toEntity(request);

        assertNotNull(task);
        assertEquals("My Task", task.getTitle());
        assertEquals("Desc", task.getDescription());
        assertEquals(5, task.getMinClassificationsPerImage());
        assertEquals(0.8, task.getConsensusThreshold());
        assertTrue(task.getRecipientGroups().contains(1L));
    }

    @Test
    void updateEntity_ShouldUpdateFields() {
        Task task = new Task();
        task.setTitle("Old Title");

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setName("New Title");

        taskMapper.updateEntity(task, request);

        assertEquals("New Title", task.getTitle());
    }

    @Test
    void toResponse_ShouldMapTaskToTaskResponse() {
        Label label = new Label();
        label.setName("Dog");
        
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Title");
        task.setStatus(TaskStatus.ACTIVE);
        task.setTargetSpecies(Collections.singletonList(label));
        task.setCreatedAt(LocalDateTime.now());

        TaskResponse response = taskMapper.toResponse(task);

        assertNotNull(response);
        assertEquals(1L, response.getTaskId());
        assertEquals("Title", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, response.getTargetSpecies().size());
        assertEquals("Dog", response.getTargetSpecies().get(0).getName());
        assertFalse(response.isAssignedToUser());
    }

    @Test
    void toResponse_ShouldMapWithAssignedToUserTrue() {
        Task task = new Task();
        task.setId(1L);
        task.setStatus(TaskStatus.ACTIVE);

        TaskResponse response = taskMapper.toResponse(task, true);

        assertTrue(response.isAssignedToUser());
    }

    @Test
    void toResponseList_ShouldMapList() {
        Task task1 = new Task();
        task1.setStatus(TaskStatus.ACTIVE);
        Task task2 = new Task();
        task2.setStatus(TaskStatus.DRAFT);

        List<TaskResponse> responses = taskMapper.toResponseList(java.util.Arrays.asList(task1, task2));

        assertEquals(2, responses.size());
    }
}
