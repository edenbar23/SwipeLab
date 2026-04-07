package com.swipelab.auth.external;

import com.swipelab.dto.request.ExternalLoginRequest;
import com.swipelab.integration.stardbi.StardbiClient;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StardbiAuthService {

    private final StardbiAuthProvider stardbiAuthProvider;
    private final StardbiClient stardbiClient;
    private final UserRepository userRepository;

    public boolean loginExternal(ExternalLoginRequest request) {
        String accessToken = request.getAccessToken();
        String username = request.getUsername();
        
        // Ensure the token is valid with Stardbi
        if (stardbiAuthProvider.validateToken(accessToken)) {
            // Cache the user details for token mapping
            stardbiAuthProvider.cacheUserDetails(accessToken, username);
            
            // Map to existing DB user if needed
            Optional<User> localUser = userRepository.findByUsername(username);
            if (localUser.isEmpty()) {
                log.warn("Stardbi user {} logged in but has no local mapping in SwipeLab DB.", username);
            }
            return true;
        }
        
        return false;
    }

    public UserDetails processStardbiToken(String accessToken) {
        // Attempt to validate via the provider
        if (stardbiAuthProvider.validateToken(accessToken)) {
            UserDetails details = stardbiAuthProvider.getUserDetails(accessToken);
            if (details != null) {
                // Return details which grants ADMIN role dynamically
                return details;
            }
        }
        return null;
    }
}
