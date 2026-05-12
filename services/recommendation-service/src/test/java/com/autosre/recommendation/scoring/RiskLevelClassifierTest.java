package com.autosre.recommendation.scoring;

import com.autosre.recommendation.model.RemediationRecommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RiskLevelClassifier")
class RiskLevelClassifierTest {

    private RiskLevelClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RiskLevelClassifier();
    }

    @Nested
    @DisplayName("classify")
    class Classify {

        @Test
        @DisplayName("returns HIGH for ROLLBACK_DEPLOYMENT action")
        void returnsHighForRollback() {
            var risk = classifier.classify(
                    "plan-1", Set.of("ROLLBACK_DEPLOYMENT"), 1);

            assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
        }

        @Test
        @DisplayName("returns HIGH for QUARANTINE_POD action")
        void returnsHighForQuarantine() {
            var risk = classifier.classify(
                    "plan-2", Set.of("QUARANTINE_POD"), 1);

            assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
        }

        @Test
        @DisplayName("returns MEDIUM for SCALE_DEPLOYMENT action")
        void returnsMediumForScale() {
            var risk = classifier.classify(
                    "plan-3", Set.of("SCALE_DEPLOYMENT"), 1);

            assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
        }

        @Test
        @DisplayName("returns MEDIUM for large blast radius")
        void returnsMediumForBlastRadius() {
            var risk = classifier.classify(
                    "plan-4", Set.of("RESTART_POD"), 5);

            assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
        }

        @Test
        @DisplayName("returns LOW for RESTART_POD only")
        void returnsLowForRestartPod() {
            var risk = classifier.classify(
                    "plan-5", Set.of("RESTART_POD"), 1);

            assertEquals(RemediationRecommendation.RiskLevel.LOW, risk);
        }

        @Test
        @DisplayName("returns MEDIUM when no action types provided")
        void returnsMediumForEmptyActions() {
            var risk = classifier.classify("plan-6", Set.of(), 1);

            assertEquals(RemediationRecommendation.RiskLevel.MEDIUM, risk);
        }
    }

    @Nested
    @DisplayName("classifyByConfidence")
    class ClassifyByConfidence {

        @Test
        @DisplayName("returns LOW for high confidence with low-risk actions")
        void returnsLowForHighConfidence() {
            var risk = classifier.classifyByConfidence(
                    0.95, Set.of("RESTART_POD", "NOTIFY_ONCALL"));

            assertEquals(RemediationRecommendation.RiskLevel.LOW, risk);
        }

        @Test
        @DisplayName("returns HIGH for low confidence")
        void returnsHighForLowConfidence() {
            var risk = classifier.classifyByConfidence(
                    0.50, Set.of("RESTART_POD"));

            assertEquals(RemediationRecommendation.RiskLevel.HIGH, risk);
        }
    }
}