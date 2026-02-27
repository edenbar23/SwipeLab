package com.swipelab.auth.infrastructure;

import com.swipelab.auth.application.JwtService;
import com.swipelab.auth.application.OAuth2Service;
import com.swipelab.users.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private OAuth2Service oAuth2Service;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @Mock
    private RedirectStrategy redirectStrategy;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "redirectUri", "http://localhost:3000/oauth2/redirect");
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    void onAuthenticationSuccess_ShouldRedirectWithTokens() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        User user = new User();
        when(oAuth2Service.processUserLogin(oAuth2User)).thenReturn(user);

        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        handler.onAuthenticationSuccess(request, response, authentication);

        String expectedUrl = "http://localhost:3000/oauth2/redirect?accessToken=access-token&refreshToken=refresh-token";
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), eq(expectedUrl));
    }
}
