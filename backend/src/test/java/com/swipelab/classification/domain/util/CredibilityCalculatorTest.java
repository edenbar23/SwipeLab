package com.swipelab.classification.domain.util;

import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CredibilityCalculatorTest {

    private CredibilityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CredibilityCalculator();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Classification createClassification(Long imageId, String querySpecies,
                                                 Classification.UserResponse response) {
        Image image = new Image();
        image.setId(imageId);
        Classification c = new Classification();
        c.setImage(image);
        c.setQuerySpecies(querySpecies);
        c.setUserResponse(response);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }

    private Classification createClassification(Long imageId, Classification.UserResponse response) {
        return createClassification(imageId, "wasp", response);
    }

    // ── normalizeKappa ────────────────────────────────────────────────────────

    @Test
    void normalizeKappa_ShouldMap_MinusOneTo0() {
        assertEquals(0.0, calculator.normalizeKappa(-1.0), 1e-9);
    }

    @Test
    void normalizeKappa_ShouldMap_ZeroToHalf() {
        assertEquals(0.5, calculator.normalizeKappa(0.0), 1e-9);
    }

    @Test
    void normalizeKappa_ShouldMap_OneTo1() {
        assertEquals(1.0, calculator.normalizeKappa(1.0), 1e-9);
    }

    // ── calculateCompositeScore ───────────────────────────────────────────────

    @Test
    void compositeScore_AllSignals_PerfectAgreement_Returns100() {
        // gold=1.0, majority=1.0, kappa=1.0, penalty=0
        double score = calculator.calculateCompositeScore(1.0, 1.0, 1.0, 0.0);
        assertEquals(100.0, score, 1e-9);
    }

    @Test
    void compositeScore_AllSignals_Zero_Returns0() {
        // kappa=-1 → normalized=0
        double score = calculator.calculateCompositeScore(0.0, 0.0, -1.0, 0.0);
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void compositeScore_MissingGold_RedistributesWeight() {
        // Only majority=1.0 and kappa=1.0 available
        double score = calculator.calculateCompositeScore(null, 1.0, 1.0, 0.0);
        assertEquals(100.0, score, 1e-9); // Full score with redistributed weights
    }

    @Test
    void compositeScore_NoSignal_Returns50() {
        double score = calculator.calculateCompositeScore(null, null, null, 0.0);
        assertEquals(50.0, score, 1e-9);
    }

    @Test
    void compositeScore_PenaltyClampsToZero() {
        // Perfect score minus huge penalty → must not go negative
        double score = calculator.calculateCompositeScore(1.0, 1.0, 1.0, 999.0);
        assertEquals(0.0, score, 1e-9);
    }

    @Test
    void compositeScore_ClampedToMax100() {
        // Already at 100 — penalty of -10 (negative) should still be clamped
        double score = calculator.calculateCompositeScore(1.0, 1.0, 1.0, -10.0);
        assertEquals(100.0, score, 1e-9);
    }

    // ── calculateCohenKappa ───────────────────────────────────────────────────

    @Test
    void cohenKappa_EmptyLists_ReturnsZero() {
        assertEquals(0.0, calculator.calculateCohenKappa(
                Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void cohenKappa_PerfectAgreement_Returns1() {
        List<Classification> user1 = Arrays.asList(
                createClassification(1L, "wasp", Classification.UserResponse.YES),
                createClassification(2L, "wasp", Classification.UserResponse.NO)
        );
        List<Classification> user2 = Arrays.asList(
                createClassification(1L, "wasp", Classification.UserResponse.YES),
                createClassification(2L, "wasp", Classification.UserResponse.NO)
        );
        assertEquals(1.0, calculator.calculateCohenKappa(user1, user2), 1e-9);
    }

    @Test
    void cohenKappa_NoCommonPairs_ReturnsZero() {
        // Same image but different query species → no common pairs
        List<Classification> user1 = List.of(
                createClassification(1L, "wasp", Classification.UserResponse.YES));
        List<Classification> user2 = List.of(
                createClassification(1L, "bee", Classification.UserResponse.YES));
        assertEquals(0.0, calculator.calculateCohenKappa(user1, user2), 1e-9);
    }

    // ── calculateMajorityVote ─────────────────────────────────────────────────

    @Test
    void majorityVote_Empty_ReturnsNull() {
        assertNull(calculator.calculateMajorityVote(Collections.emptyList()));
    }

    @Test
    void majorityVote_SingleClassification_ReturnsNull() {
        assertNull(calculator.calculateMajorityVote(
                List.of(createClassification(1L, Classification.UserResponse.YES))));
    }

    @Test
    void majorityVote_ClearMajority_ReturnsWinner() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );
        assertEquals(Classification.UserResponse.YES,
                calculator.calculateMajorityVote(classifications));
    }

    @Test
    void majorityVote_TiedVote_ReturnsNull() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );
        assertNull(calculator.calculateMajorityVote(classifications));
    }

    // ── calculateMajorityAgreementScore ──────────────────────────────────────

    @Test
    void majorityAgreement_Matches_Returns1() {
        Classification c = createClassification(1L, Classification.UserResponse.YES);
        assertEquals(1.0, calculator.calculateMajorityAgreementScore(
                c, Classification.UserResponse.YES));
    }

    @Test
    void majorityAgreement_Disagrees_Returns0() {
        Classification c = createClassification(1L, Classification.UserResponse.YES);
        assertEquals(0.0, calculator.calculateMajorityAgreementScore(
                c, Classification.UserResponse.NO));
    }

    @Test
    void majorityAgreement_NullMajority_Returns0() {
        Classification c = createClassification(1L, Classification.UserResponse.YES);
        assertEquals(0.0, calculator.calculateMajorityAgreementScore(c, null));
    }

    // ── calculateConsensusStrength ────────────────────────────────────────────

    @Test
    void consensusStrength_TwoToOne_Returns0point66() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );
        assertEquals(2.0 / 3.0, calculator.calculateConsensusStrength(classifications), 1e-9);
    }

    @Test
    void consensusStrength_Tied_Returns0point5() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );
        assertEquals(0.5, calculator.calculateConsensusStrength(classifications), 1e-9);
    }

    // ── calculateWeightedCohenKappa ───────────────────────────────────────────

    @Test
    void weightedKappa_OnlyLast50Classifications_AreUsed() {
        List<Classification> user1 = new ArrayList<>();
        List<Classification> user2 = new ArrayList<>();

        // 55 pairs where user1 agrees with user2
        for (int i = 0; i < 55; i++) {
            user1.add(createClassification((long) i, Classification.UserResponse.YES));
            user2.add(createClassification((long) i, Classification.UserResponse.YES));
        }

        double kappa = calculator.calculateWeightedCohenKappa(user1, user2);
        assertEquals(1.0, kappa, 1e-9);
    }
}
