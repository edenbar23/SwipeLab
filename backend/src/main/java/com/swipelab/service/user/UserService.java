package com.swipelab.service.user;

import com.swipelab.repository.UserRepository;
import org.springframework.stereotype.Service;


import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import com.swipelab.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getUserProfile(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return UserProfileResponse.builder()
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }
}
