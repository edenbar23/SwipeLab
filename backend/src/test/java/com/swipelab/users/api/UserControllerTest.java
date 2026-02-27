package com.swipelab.users.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.dto.request.UpdateProfileRequest;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.users.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

        profileResponse = UserProfileResponse.builder()
                .username("testuser")
                .role(com.swipelab.model.enums.UserRole.USER)
                .build();
    }

    @Test
    void getCurrentUserProfile_ShouldReturnResponse() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserProfile_ShouldReturnResponse() throws Exception {
        when(userService.getUserProfile("testuser")).thenReturn(profileResponse);

        mockMvc.perform(get("/api/v1/users/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void updateProfile_ShouldReturnUpdatedResponse() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        
        when(userService.updateUserProfile(any(UpdateProfileRequest.class))).thenReturn(profileResponse);

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getAllUsers_ShouldReturnList() throws Exception {
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(profileResponse));

        mockMvc.perform(get("/api/v1/users/get-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    void banUser_ShouldReturnResponse() throws Exception {
        when(userService.banUser("testuser")).thenReturn(profileResponse);

        mockMvc.perform(post("/api/v1/users/ban/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void unbanUser_ShouldReturnResponse() throws Exception {
        when(userService.unbanUser("testuser")).thenReturn(profileResponse);

        mockMvc.perform(post("/api/v1/users/unban/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }
}
