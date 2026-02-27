package com.swipelab.classification.domain.util;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredibilityCalculatorTest {

    private CredibilityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CredibilityCalculator();
    }

    private Classification createClassification(Long imageId, Classification.UserResponse response) {
        Image image = new Image();
        image.setId(imageId);

        Classification classification = new Classification();
        classification.setImage(image);
        classification.setUserResponse(response);
        classification.setCreatedAt(LocalDateTime.now());
        return classification;
    }

    @Test
    void calculateCohenKappa_ShouldReturnZero_WhenEmptyLists() {
        double kappa = calculator.calculateCohenKappa(Collections.emptyList(), Collections.emptyList());
        assertEquals(0.0, kappa);
    }

    @Test
    void calculateCohenKappa_ShouldCalculateCorrectly_WhenPerfectAgreement() {
        List<Classification> user1 = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(2L, Classification.UserResponse.NO)
        );
        List<Classification> user2 = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(2L, Classification.UserResponse.NO)
        );

        double kappa = calculator.calculateCohenKappa(user1, user2);
        assertEquals(1.0, kappa);
    }

    @Test
    void calculateMajorityVote_ShouldReturnNull_WhenEmpty() {
        Classification.UserResponse result = calculator.calculateMajorityVote(Collections.emptyList());
        assertEquals(null, result);
    }

    @Test
    void calculateMajorityVote_ShouldReturnMajority() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );

        Classification.UserResponse result = calculator.calculateMajorityVote(classifications);
        assertEquals(Classification.UserResponse.YES, result);
    }

    @Test
    void calculateMajorityVote_ShouldReturnNull_WhenNoClearMajority() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO)
        );

        Classification.UserResponse result = calculator.calculateMajorityVote(classifications);
        assertEquals(null, result);
    }

    @Test
    void calculateMajorityAgreementScore_ShouldReturnScore() {
        Classification userClass = createClassification(1L, Classification.UserResponse.YES);
        
        double score = calculator.calculateMajorityAgreementScore(userClass, Classification.UserResponse.YES);
        assertEquals(1.0, score);

        score = calculator.calculateMajorityAgreementScore(userClass, Classification.UserResponse.NO);
        assertEquals(0.0, score);
        
        score = calculator.calculateMajorityAgreementScore(userClass, null);
        assertEquals(0.0, score);
    }

    @Test
    void calculateConsensusStrength_ShouldCalculateProperly() {
        List<Classification> classifications = Arrays.asList(
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.YES),
                createClassification(1L, Classification.UserResponse.NO),
                createClassification(1L, Classification.UserResponse.NO)
        );

        double strength = calculator.calculateConsensusStrength(classifications);
        assertEquals(0.5, strength);
    }

    @Test
    void calculateWeightedCohenKappa_ShouldOnlyConsiderLast50() {
        List<Classification> user1 = new ArrayList<>();
        List<Classification> user2 = new ArrayList<>();

        for (int i = 0; i < 55; i++) {
            Classification c1 = createClassification((long) i, Classification.UserResponse.YES);
            Classification c2 = createClassification((long) i, Classification.UserResponse.YES);
            user1.add(c1);
            user2.add(c2);
        }

        double kappa = calculator.calculateWeightedCohenKappa(user1, user2);
        assertEquals(1.0, kappa);
    }
}
