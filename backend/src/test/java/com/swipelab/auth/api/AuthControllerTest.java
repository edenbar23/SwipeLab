package com.swipelab.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.auth.application.AuthenticationService;
import com.swipelab.auth.application.JwtService;
import com.swipelab.auth.application.OAuth2Service;
import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.request.EmailVerificationRequest;
import com.swipelab.dto.request.ForgotPasswordRequest;
import com.swipelab.dto.request.LoginRequest;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.request.ResetPasswordRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.users.application.UserService;
import com.swipelab.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @Mock
    private OAuth2Service oAuth2Service;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void register_ShouldReturn201_WhenRegistrationIsSuccessful() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@swipelab.com");
        request.setPassword("password123");
        request.setDisplayName("Test User");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    void verifyEmail_ShouldReturn200_WhenTokenIsValid() throws Exception {
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken("valid-token");

        doNothing().when(authenticationService).verifyEmail("valid-token");

        mockMvc.perform(post("/api/v1/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(authenticationService, times(1)).verifyEmail("valid-token");
    }

    @Test
    void resendVerificationEmail_ShouldReturn200() throws Exception {
        doNothing().when(authenticationService).resendVerificationEmail("test@swipelab.com");

        mockMvc.perform(post("/api/v1/auth/email/resend")
                .param("email", "test@swipelab.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(authenticationService, times(1)).resendVerificationEmail("test@swipelab.com");
    }

    @Test
    void getCurrentUser_ShouldReturnUnauthenticated_WhenPrincipalIsNull() throws Exception {
        mockMvc.perform(get("/api/v1/auth/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void getCurrentUser_ShouldReturnUserDetails_WhenPrincipalExists() throws Exception {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@swipelab.com");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptySet());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            mockMvc.perform(get("/api/v1/auth/user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.email").value("test@swipelab.com"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void login_ShouldReturn200_WhenCredentialsAreValid() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access")
                .refreshToken("refresh")
                .build();

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));

        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    void testEndpoint_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Security configuration is working!"));
    }

    @Test
    void me_ShouldReturnUserProfile_WhenAuthenticated() throws Exception {
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("testuser");

        UserProfileResponse response = UserProfileResponse.builder()
                .username("testuser")
                .build();

        when(userService.getUserProfile("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/me")
                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void forgotPassword_ShouldReturn200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@swipelab.com");

        doNothing().when(authenticationService).forgotPassword("test@swipelab.com");

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(authenticationService, times(1)).forgotPassword("test@swipelab.com");
    }

    @Test
    void resetPassword_ShouldReturn200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newpassword");

        doNothing().when(authenticationService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(authenticationService, times(1)).resetPassword(any(ResetPasswordRequest.class));
    }
}
