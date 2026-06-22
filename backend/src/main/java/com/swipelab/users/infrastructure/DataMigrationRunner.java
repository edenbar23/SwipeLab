package com.swipelab.users.infrastructure;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.CredibilityRecord;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CredibilityRepository credibilityRepository;
    private final ClassificationRepository classificationRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Running User Status data migration...");
        List<User> users = userRepository.findAll();
        int updated = 0;

        for (User user : users) {
             // For newly loaded legacy users where status defaults to PENDING_VERIFICATION (or is null), 
             // we evaluate the exact combination of boolean flags to set the truthful STATUS value.
             // We do this blindly to ensure all existing users are verified against the old flags.
             
             UserStatus determinedStatus;

             if (!Boolean.TRUE.equals(user.getActive())) {
                 determinedStatus = UserStatus.BANNED;
             } else if (Boolean.TRUE.equals(user.getEmailVerified())) {
                 determinedStatus = UserStatus.ACTIVE;
             } else {
                 determinedStatus = UserStatus.PENDING_VERIFICATION;
             }
             
             // Check if it actually needs updating (to avoid useless DB writes on every boot) 
             // Wait, because we put a builder default in User.java, a new uninitialized entity will have PENDING_VERIFICATION.
             if (user.getStatus() == null || user.getStatus() != determinedStatus) {
                 user.setStatus(determinedStatus);
                 userRepository.save(user);
                 updated++;
             }
        }
        log.info("Data migration completed. Updated {} users.", updated);

        log.info("Running Gold Image Classification backfill migration...");
        List<CredibilityRecord> records = credibilityRepository.findAll();
        int backfilled = 0;
        for (CredibilityRecord record : records) {
            boolean exists = classificationRepository.existsByUsernameAndImageId(record.getUsername(), record.getGoldImage().getImage().getId());
            if (!exists) {
                // Determine user role (fallback to USER if not found)
                String role = userRepository.findByUsername(record.getUsername())
                        .map(u -> u.getRole().name())
                        .orElse("USER");

                Classification classification = Classification.builder()
                        .username(record.getUsername())
                        .userRole(role)
                        .taskId(record.getTaskId())
                        .image(record.getGoldImage().getImage())
                        .querySpecies(record.getQuerySpecies())
                        .userResponse(record.getUserResponse())
                        .build();
                classificationRepository.save(classification);
                backfilled++;
            }
        }
        log.info("Gold Image Classification backfill completed. Inserted {} missing classifications.", backfilled);
    }
}
