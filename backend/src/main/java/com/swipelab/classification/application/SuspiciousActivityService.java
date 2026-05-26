package com.swipelab.classification.application;

import com.swipelab.classification.domain.SuspiciousActivityRecord;
import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.dto.response.SuspiciousActivityResponse;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for querying and managing the suspicious activity audit log.
 * Used by SuspiciousActivityController (admin-facing).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspiciousActivityService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SuspiciousActivityResponse> getAllActivity() {
        return suspiciousActivityRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SuspiciousActivityResponse> getActivityForUser(String username) {
        return suspiciousActivityRepository
                .findByUsernameOrderByCreatedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Clears a user's strike counter and resets their status to ACTIVE.
     * Called by Super Admin when a flagging is determined to be a false positive.
     */
    @Transactional
    public void resetStrikes(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.swipelab.exception.ResourceNotFoundException(
                        "User not found: " + username));

        user.setStrikeCount(0);
        user.setWarningCount(0);
        user.setConsecutiveCorrectGolds(0);
        user.setStatus(com.swipelab.model.enums.UserStatus.ACTIVE);
        user.setLastWarningAt(null);

        userRepository.save(user);
        log.info("Strikes and warnings reset for user {} by admin", username);
    }

    private SuspiciousActivityResponse toResponse(SuspiciousActivityRecord r) {
        return SuspiciousActivityResponse.builder()
                .id(r.getId())
                .username(r.getUsername())
                .reason(r.getReason())
                .responseTimeMs(r.getResponseTimeMs())
                .taskId(r.getTaskId())
                .severity(r.getSeverity())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
