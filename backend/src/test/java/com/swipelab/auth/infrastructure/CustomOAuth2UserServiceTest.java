package com.swipelab.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomOAuth2UserServiceTest {

    // Since CustomOAuth2UserService just overrides loadUser to call super.loadUser(),
    // testing it directly would require mocking the actual Spring HTTP request to the OAuth2 provider.
    // Here we just test instantiation and basic expected failure when passing invalid request.

    @Test
    void loadUser_ShouldThrowException_WhenInvalidRequest() {
        CustomOAuth2UserService service = new CustomOAuth2UserService();

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("google")
                .clientId("clientId")
                .clientSecret("clientSecret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        OAuth2UserRequest request = new OAuth2UserRequest(clientRegistration, accessToken);

        // Expect Exception because super.loadUser() attempts to make a real HTTP request to userInfoUri
        assertThrows(Exception.class, () -> service.loadUser(request));
    }
}
