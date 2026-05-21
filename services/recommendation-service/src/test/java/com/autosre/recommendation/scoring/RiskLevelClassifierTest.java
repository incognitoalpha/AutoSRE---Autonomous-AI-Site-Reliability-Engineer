package com.autosre.recommendation.scoring;

import com.autosre.recommendation.model.RemediationRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskLevelClassifierTest {

    private RiskLevelClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RiskLevelClassifier();
    }

    @Test
    void testClassify_NullActions() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", null, 1);
        assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
    }

    @Test
    void testClassify_EmptyActions() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", Collections.emptySet(), 1);
        assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
    }

    @Test
    void testClassify_HighRisk() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", Set.of("ROLLBACK_DEPLOYMENT"), 1);
        assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
    }

    @Test
    void testClassify_MediumRisk() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", Set.of("SCALE_DEPLOYMENT"), 1);
        assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
    }

    @Test
    void testClassify_LowRisk() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", Set.of("RESTART_POD"), 1);
        assertEquals(RemediationRecommendation.RiskLevel.LOW, risk);
    }

    @Test
    void testClassify_HighBlastRadius() {
        RemediationRecommendation.RiskLevel risk = classifier.classify("plan1", Set.of("RESTART_POD"), 4);
        assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
    }

    @Test
    void testClassifyByConfidence_LowRisk() {
        RemediationRecommendation.RiskLevel risk = classifier.classifyByConfidence(0.95, Set.of("RESTART_POD"));
        assertEquals(RemediationRecommendation.RiskLevel.LOW, risk);
    }

    @Test
    void testClassifyByConfidence_MediumRisk() {
        RemediationRecommendation.RiskLevel risk = classifier.classifyByConfidence(0.80, Set.of("SCALE_DEPLOYMENT"));
        assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
    }

    @Test
    void testClassifyByConfidence_HighRiskDueToActions() {
        RemediationRecommendation.RiskLevel risk = classifier.classifyByConfidence(0.95, Set.of("ROLLBACK_DEPLOYMENT"));
        assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
    }

    @Test
    void testClassifyByConfidence_HighRiskDueToConfidence() {
        RemediationRecommendation.RiskLevel risk = classifier.classifyByConfidence(0.50, Set.of("RESTART_POD"));
        assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
    }
}
