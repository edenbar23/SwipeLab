package com.swipelab.auth.external;

import org.springframework.security.core.userdetails.UserDetails;

public interface ExternalAuthProvider {
    boolean supports(String providerName);
    boolean validateToken(String accessToken);
    String refreshToken(String refreshToken);
    UserDetails getUserDetails(String accessToken);
}
