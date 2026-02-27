package com.swipelab.users.api;

import com.swipelab.dto.response.dashboard.MyTaskDetailsResponse;
import com.swipelab.dto.response.dashboard.MyTasksPageResponse;
import com.swipelab.dto.response.dashboard.PlayTaskResponse;
import com.swipelab.users.application.UserDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserDashboardService userDashboardService;

    @InjectMocks
    private UserDashboardController userDashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userDashboardController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void getMyTasks_ShouldReturnResponse() throws Exception {
        MyTasksPageResponse response = new MyTasksPageResponse();
        // Since we don't have setters right now, we can just mock the whole response

        when(userDashboardService.getMyTasks(1, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/my-tasks")
                .param("page", "1")
                .param("pageSize", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void getTaskDetails_ShouldReturnResponse() throws Exception {
        MyTaskDetailsResponse response = new MyTaskDetailsResponse();

        when(userDashboardService.getTaskDetails(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/my-tasks/{taskId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void playTask_ShouldReturnResponse() throws Exception {
        PlayTaskResponse response = new PlayTaskResponse();

        when(userDashboardService.playTask(1L, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/my-tasks/{taskId}/play", 1L)
                .param("count", "10"))
                .andExpect(status().isOk());
    }
}
