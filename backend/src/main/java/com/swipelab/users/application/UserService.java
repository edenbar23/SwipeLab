package com.swipelab.users.application;

import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.users.dto.UpdateProfileRequest;
import com.swipelab.users.dto.UserProfileResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserStatusChangedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthMapper authMapper;
    private final SecurityAuthorizationService securityAuthorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final GamificationRepository gamificationRepository;

    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return withCurrentStreak(authMapper.toUserProfileResponse(user), user.getUsername());
    }

    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return withCurrentStreak(authMapper.toUserProfileResponse(user), user.getUsername());
    }

    /**
     * The current streak lives on the {@link Gamification} entity (not denormalized onto
     * {@link User}), so hydrate it onto the profile response here. Defaults to 0 when the
     * user has no gamification row yet.
     */
    private UserProfileResponse withCurrentStreak(UserProfileResponse response, String username) {
        Integer streak = gamificationRepository.findById(username)
                .map(Gamification::getCurrentStreak)
                .orElse(0);
        response.setCurrentStreak(streak != null ? streak : 0);
        return response;
    }

    @Transactional
    public UserProfileResponse updateUserProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        User updatedUser = userRepository.save(user);
        return authMapper.toUserProfileResponse(updatedUser);
    }

    public Double getUserCredibility(String username) {
        return userRepository.findByUsername(username)
                .map(User::getCredibilityScore)
                .orElse(0.0);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        // In our JWT setup, principal is usually the User entity or UserDetails
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            // If SecurityContext stores the entity directly
            return (User) principal;
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // If it stores UserDetails, fetch by username
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        }

        // Fallback for string principal
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(authMapper::toUserProfileResponse)
                .collect(Collectors.toList());
    }

    public List<UserProfileResponse> getUsersByRole(String roleName) {
        try {
            com.swipelab.model.enums.UserRole role = com.swipelab.model.enums.UserRole.valueOf(roleName.toUpperCase());
            return userRepository.findByRole(role).stream()
                    .filter(user -> !securityAuthorizationService.isSuperAdmin(user.getUsername()))
                    .map(authMapper::toUserProfileResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid role: " + roleName);
        }
    }

    @Transactional
    public UserProfileResponse banUser(String username) {
        // Super Admin can never be banned — guard against both manual and automated paths.
        if (securityAuthorizationService.isSuperAdmin(username)) {
            throw new IllegalArgumentException("Super Admin cannot be banned.");
        }
        User currentUser = getCurrentUser();
        if (currentUser.getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("You cannot ban yourself.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        user.setActive(false);
        user.setStatus(com.swipelab.model.enums.UserStatus.BANNED);
        // Mirror the auto-ban path — accountLocked blocks login and the BannedUserFilter
        user.setAccountLocked(true);
        User updatedUser = userRepository.save(user);

        // Notify the recipients module so the user is removed from active recipient lists
        eventPublisher.publishEvent(UserStatusChangedEvent.builder()
                .username(username)
                .active(false)
                .build());

        return authMapper.toUserProfileResponse(updatedUser);
    }

    @Transactional
    public UserProfileResponse unbanUser(String username) {
        User currentUser = getCurrentUser();
        if (currentUser.getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("You cannot unban yourself.");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        user.setActive(true);
        user.setAccountLocked(false);
        user.setStatus(com.swipelab.model.enums.UserStatus.ACTIVE);
        User updatedUser = userRepository.save(user);
        return authMapper.toUserProfileResponse(updatedUser);
    }
}




