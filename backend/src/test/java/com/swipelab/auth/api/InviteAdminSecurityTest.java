package com.swipelab.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.auth.application.AuthenticationService;
import com.swipelab.auth.application.JwtService;
import com.swipelab.auth.application.OAuth2Service;
import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.auth.infrastructure.BannedUserFilter;
import com.swipelab.auth.infrastructure.CustomOAuth2UserService;
import com.swipelab.auth.infrastructure.JwtAuthenticationFilter;
import com.swipelab.auth.infrastructure.OAuth2AuthenticationFailureHandler;
import com.swipelab.auth.infrastructure.OAuth2AuthenticationSuccessHandler;
import com.swipelab.auth.infrastructure.RateLimitingFilter;
import com.swipelab.auth.dto.InviteAdminRequest;
import com.swipelab.users.application.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security slice test for the admin invite endpoint.
 *
 * Uses @WebMvcTest so Spring Security's full method-security interceptor
 * is active and @PreAuthorize is evaluated.
 *
 * Key design decisions:
 * - SecurityAuthorizationService is a @MockBean so we control isSuperAdmin()
 *   return values precisely. @Import loses the bean name qualifier needed
 *   for the SpEL "@securityAuthorizationService" reference.
 * - .with(csrf()) is required because @WebMvcTest re-enables CSRF by default
 *   even though SecurityConfig disables it in production.
 * - All SecurityConfig @Autowired filter dependencies are provided as @MockBean
 *   (no-op stubs) so the application context loads cleanly.
 *
 * Covers:
 *  - Edge: anonymous caller (unauthenticated) → 403
 *  - Edge: authenticated USER where isSuperAdmin returns false → 403
 *  - Edge: authenticated RESEARCHER where isSuperAdmin returns false → 403
 *  - Happy: authenticated caller where isSuperAdmin returns true → 200, service invoked
 */
@WebMvcTest(AuthController.class)
@Import({
    com.swipelab.auth.infrastructure.SecurityConfig.class,
    SecurityAuthorizationService.class
})
@TestPropertySource(properties = {
        "app.security.super-admin.username=superadmin@swipelab.com",
        "cors.allowed-origins=http://localhost:3000",
        "app.frontend-url=http://localhost:3000"
})
@DisplayName("POST /api/v1/auth/invitation/admin — @PreAuthorize guard")
class InviteAdminSecurityTest {

    private static final String ENDPOINT = "/api/v1/auth/invitation/admin";

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // ── AuthController collaborators ──────────────────────────────────────────
    @MockBean private AuthenticationService              authenticationService;
    @MockBean private UserService                        userService;
    @MockBean private OAuth2Service                      oAuth2Service;
    @MockBean private AuthMapper                         authMapper;
    @MockBean private JwtService                         jwtService;

    // ── SecurityConfig filter dependencies ────────────────────────────────────
    @MockBean private JwtAuthenticationFilter            jwtAuthenticationFilter;
    @MockBean private BannedUserFilter                   bannedUserFilter;
    @MockBean private RateLimitingFilter                 rateLimitingFilter;
    @MockBean private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    @MockBean private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    @MockBean private CustomOAuth2UserService            customOAuth2UserService;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @org.junit.jupiter.api.BeforeEach
    void setupFilterChains() throws Exception {
        // Since the filters are mocked, their doFilter methods do nothing by default,
        // which swallows the request and returns an empty 200 OK without reaching the controller.
        // We must configure them to proceed down the chain.
        org.mockito.stubbing.Answer<Void> proceed = invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        };

        doAnswer(proceed).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(proceed).when(bannedUserFilter).doFilter(any(), any(), any());
        doAnswer(proceed).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String validBody() throws Exception {
        InviteAdminRequest req = new InviteAdminRequest();
        req.setEmail("new.researcher@example.com");
        return objectMapper.writeValueAsString(req);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @WithAnonymousUser
    @DisplayName("anonymous caller is rejected with 401")
    void anonymousCaller_isRejected() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, never()).inviteAdmin(any());
    }

    @Test
    @WithMockUser(username = "regular_user", roles = "USER")
    @DisplayName("USER role is rejected with 403")
    void regularUser_isRejected() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).inviteAdmin(any());
    }

    @Test
    @WithMockUser(username = "researcher_not_superadmin", roles = "RESEARCHER")
    @DisplayName("RESEARCHER who is NOT super-admin is rejected with 403")
    void researcher_nonSuperAdmin_isRejected() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(authenticationService, never()).inviteAdmin(any());
    }

    // ── Happy flow ────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "superadmin@swipelab.com", roles = "RESEARCHER")
    @DisplayName("super-admin is allowed and service is invoked exactly once")
    void superAdmin_isAllowedAndServiceInvoked() throws Exception {
        doNothing().when(authenticationService).inviteAdmin(any(InviteAdminRequest.class));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody())
                        .with(csrf()))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        verify(authenticationService, times(1)).inviteAdmin(any(InviteAdminRequest.class));
    }
}

