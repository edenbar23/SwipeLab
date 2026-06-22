package com.swipelab.users.application;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.model.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens to ClassificationSubmittedEvent and triggers credibility recalculation
 * at the right time — fixing the dead-code problem where CredibilityService was
 * never invoked.
 *
 * Trigger rules:
 *
 *   1. CONSENSUS THRESHOLD CROSSED
 *      When a (imageId, querySpecies) pair reaches minClassificationsForConsensus
 *      classifications for the first time, recalculate credibility for ALL users
 *      who classified that pair (they now have a meaningful consensus to compare against).
 *
 *   2. EXPERT CLASSIFICATION
 *      When a RESEARCHER classifies an image-query pair, recalculate credibility for
 *      all regular users who already classified it (new ground truth available).
 *
 *   3. SINGLE USER UPDATE (default)
 *      For every other regular-user classification, update only the submitting user's
 *      credibility so the score stays fresh.
 *
 * All handling is @Async so it never blocks the labeling request thread.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CredibilityEventListener {

    private final CredibilityService credibilityService;
    private final ClassificationRepository classificationRepository;

    @Value("${app.credibility.min-classifications-for-consensus:3}")
    private int minClassificationsForConsensus;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClassificationSubmitted(ClassificationSubmittedEvent event) {
        // Gold-image classifications are handled separately by GoldImageEvaluatorService
        // (they feed CredibilityRecord). Still trigger regular credibility refresh below.

        String username = event.getUsername();
        Long imageId    = event.getImageId();
        String species  = event.getSpecies();
        String userRole = event.getUserRole();

        if (username == null || imageId == null) {
            log.warn("CredibilityEventListener: missing username or imageId in event — skipping");
            return;
        }

        boolean isExpert = UserRole.RESEARCHER.name().equalsIgnoreCase(userRole);

        if (isExpert) {
            // Rule 2: Expert classified — recalculate all non-expert users for this pair
            log.info("Expert classification on image={} query={} — recalculating affected users",
                    imageId, species);
            credibilityService.recalculateCredibilityForImageQuery(imageId, species);
            return;
        }

        if (event.isGoldStandard()) {
            // Gold image result is already stored in CredibilityRecord by GoldImageEvaluatorService.
            // Just refresh this user's composite score to pick up the new gold result.
            credibilityService.updateUserCredibility(username);
            return;
        }

        // Rule 1: Check if consensus threshold just crossed for this (imageId, querySpecies) pair
        long pairCount = classificationRepository
                .countByImageIdAndQuerySpecies(imageId, nullToEmpty(species));

        if (pairCount == minClassificationsForConsensus) {
            // Threshold just crossed — recalculate everyone on this pair
            log.info("Consensus threshold ({}) reached for image={} query={} — recalculating all users",
                    minClassificationsForConsensus, imageId, species);
            credibilityService.recalculateCredibilityForImageQuery(imageId, species);
        } else if (pairCount > minClassificationsForConsensus) {
            // Pair already had consensus — just update the submitting user
            log.debug("Updating credibility for user {} after classification on image={} query={}",
                    username, imageId, species);
            credibilityService.updateUserCredibility(username);
        } else {
            // Below threshold — no consensus yet, skip credibility update
            log.debug("Image={} query={} has {}/{} classifications — below threshold, skipping credibility",
                    imageId, species, pairCount, minClassificationsForConsensus);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
