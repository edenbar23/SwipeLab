package com.swipelab.auth.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException exception;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/login");
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationFailure_ShouldRedirectWithErrorMessage() throws Exception {
        when(exception.getLocalizedMessage()).thenReturn("OAuth failed");

        handler.onAuthenticationFailure(request, response, exception);

        verify(redirectStrategy).sendRedirect(eq(request), eq(response), eq("http://localhost:3000/login?error=OAuth failed"));
    }
}
