package com.swipelab.tasks.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.tasks.application.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private UserDetails userDetails;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(taskController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver(), new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        userDetails = new User("testuser", "password", Collections.singletonList(new SimpleGrantedAuthority("USER")));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        taskResponse = TaskResponse.builder()
                .taskId(1L)
                .name("Test Task")
                .description("Desc")
                .build();
    }

    @Test
    void getMyTasks_ShouldReturnTaskPageResponse() throws Exception {
        TaskPageResponse pageResponse = TaskPageResponse.builder()
                .tasks(Collections.singletonList(taskResponse))
                .totalTasks(1)
                .totalPages(1)
                .build();

        when(taskService.getTasksForUser(eq("testuser"), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/tasks/my-tasks")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.tasks[0].name").value("Test Task"));
    }

    @Test
    void getMyTaskDetails_ShouldReturnTaskResponse() throws Exception {
        when(taskService.getTaskForUser(1L, "testuser")).thenReturn(taskResponse);

        mockMvc.perform(get("/api/v1/tasks/my-tasks/{taskId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1L))
                .andExpect(jsonPath("$.name").value("Test Task"));
    }

    @Test
    void getAdminDashboard_ShouldReturnListOfTaskResponse() throws Exception {
        when(taskService.getAdminDashboard()).thenReturn(Collections.singletonList(taskResponse));

        mockMvc.perform(get("/api/v1/tasks/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(1L))
                .andExpect(jsonPath("$[0].name").value("Test Task"));
    }

    @Test
    void getTaskDetailsAdmin_ShouldReturnTaskResponse() throws Exception {
        when(taskService.getTaskDetailsAdmin(1L)).thenReturn(taskResponse);

        mockMvc.perform(get("/api/v1/tasks/dashboard/{taskId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1L));
    }

    @Test
    void createTask_ShouldReturnCreated() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("New Task");

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(taskResponse);

        mockMvc.perform(post("/api/v1/tasks/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(1L));
    }

    @Test
    void archiveTask_ShouldReturnOk() throws Exception {
        when(taskService.archiveTask(1L)).thenReturn(taskResponse);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/archive", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void updateTask_ShouldReturnOk() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setName("Updated");

        when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(taskResponse);

        mockMvc.perform(put("/api/v1/tasks/{taskId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void activateTask_ShouldReturnOk() throws Exception {
        when(taskService.activateTask(1L)).thenReturn(taskResponse);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/activate", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void pauseTask_ShouldReturnOk() throws Exception {
        when(taskService.pauseTask(1L)).thenReturn(taskResponse);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/pause", 1L))
                .andExpect(status().isOk());
    }
}
