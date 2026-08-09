package com.swipelab.tasks.domain;

import com.swipelab.tasks.application.port.out.TargetSpeciesProvider;
import com.swipelab.tasks.dto.CreateTaskRequest;
import com.swipelab.tasks.dto.UpdateTaskRequest;
import com.swipelab.tasks.dto.TaskResponse;
import com.swipelab.tasks.dto.TaskProgressResponse;
import com.swipelab.tasks.dto.TargetSpeciesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskMapperTest {

    private TaskMapper taskMapper;
    private TargetSpeciesProvider targetSpeciesProvider;

    @BeforeEach
    void setUp() {
        targetSpeciesProvider = Mockito.mock(TargetSpeciesProvider.class);
        taskMapper = new TaskMapper(targetSpeciesProvider);
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
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Title");
        task.setStatus(TaskStatus.ACTIVE);
        task.setTargetSpeciesIds(Collections.singletonList(2L));
        task.setCreatedAt(LocalDateTime.now());

        TargetSpeciesResponse speciesResponse = TargetSpeciesResponse.builder().name("Dog").build();
        Mockito.when(targetSpeciesProvider.getSpeciesByIdsAndRefImages(Collections.singletonList(2L), Collections.emptyList()))
               .thenReturn(Collections.singletonList(speciesResponse));

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
    void toResponse_ShouldUseProvidedProgress_NotEmpty() {
        // Regression guard: the 3-arg overload must surface the computed progress it is
        // handed, not hardcode an empty (0/0) one. The researcher TaskCard / TaskDetails
        // screens read this endpoint, so dropping the progress shows "0 of 0" forever.
        Task task = new Task();
        task.setId(7L);
        task.setStatus(TaskStatus.ACTIVE);

        TaskProgressResponse progress = new TaskProgressResponse(9, 1);

        TaskResponse response = taskMapper.toResponse(task, false, progress);

        assertNotNull(response.getProgress());
        assertEquals(9, response.getProgress().getTotalImages());
        assertEquals(1, response.getProgress().getImagesClassified());
    }

    @Test
    void toResponse_ShouldFallBackToEmptyProgress_WhenNull() {
        Task task = new Task();
        task.setId(8L);
        task.setStatus(TaskStatus.ACTIVE);

        TaskResponse response = taskMapper.toResponse(task, false, null);

        assertNotNull(response.getProgress());
        assertEquals(0, response.getProgress().getTotalImages());
        assertEquals(0, response.getProgress().getImagesClassified());
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

