package com.swipelab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.dto.request.LoginRequest;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.service.auth.AuthenticationService;
import com.swipelab.service.auth.JwtService;
import com.swipelab.service.auth.OAuth2Service;
import com.swipelab.service.user.UserService;
import com.swipelab.mapper.AuthMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit testing
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private AuthenticationService authenticationService;

        @MockBean
        private UserService userService;

        @MockBean
        private OAuth2Service oAuth2Service;

        @MockBean
        private AuthMapper authMapper;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private com.swipelab.security.JwtTokenProvider jwtTokenProvider;

        @MockBean
        private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

        @Test
        void register_ShouldReturnCreated() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setUsername("newuser");
                request.setEmail("new@example.com");
                request.setPassword("password123");
                request.setDisplayName("New User");

                AuthResponse response = AuthResponse.builder()
                                .accessToken("access")
                                .refreshToken("refresh")
                                .build();

                when(authenticationService.register(any(RegisterRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.accessToken").value("access"));
        }

        @Test
        void login_ShouldReturnOk() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("testuser");
                request.setPassword("password123");

                AuthResponse response = AuthResponse.builder()
                                .accessToken("access")
                                .build();

                when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access"));
        }
}
