package com.swipelab.security;

import com.swipelab.config.JwtConfig;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import com.swipelab.security.enums.TokenType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UserRepository userRepository;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Find user by email to get username and role
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 authentication"));

        // Generate JWT token using username and role
        String token = tokenProvider.generateToken(
                user.getUsername(),
                user.getRole().name(),
                TokenType.ACCESS,
                Duration.ofMinutes(jwtConfig.getAccessTokenExpirationMinutes())
        );

        // Get the first allowed origin as redirect target
        String redirectUri = allowedOrigins.split(",")[0].trim();

        return UriComponentsBuilder.fromUriString(redirectUri + "/oauth2/redirect")
                .queryParam("token", token)
                .build()
                .toUriString();
    }
}