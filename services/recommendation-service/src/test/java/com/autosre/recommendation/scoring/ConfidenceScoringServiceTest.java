package com.autosre.recommendation.scoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ConfidenceScoringService")
class ConfidenceScoringServiceTest {

    private ConfidenceScoringService service;

    @BeforeEach
    void setUp() {
        service = new ConfidenceScoringService();
    }

    @Nested
    @DisplayName("computeScore")
    class ComputeScore {

        @Test
        @DisplayName("returns base confidence when no modifiers apply")
        void returnsBaseWhenNoModifiers() {
            double score = service.computeScore(
                    0.75, "unknown_root_cause", false, false, 0);

            assertEquals(0.75, score, 0.01);
        }

        @Test
        @DisplayName("boosts score when runbook match found")
        void boostsScoreForRunbookMatch() {
            double score = service.computeScore(
                    0.75, "memory_exhaustion", true, false, 0);

            assertTrue(score > 0.75, "Score should be boosted for runbook match");
        }

        @Test
        @DisplayName("penalizes score for low historical resolution rate")
        void penalizesScoreForLowResolution() {
            double score = service.computeScore(
                    0.75, "network_issue", false, false, 0);

            assertTrue(score < 0.75, "Score should be penalized for low resolution rate");
        }

        @Test
        @DisplayName("boosts score when similar incidents exist")
        void boostsScoreForSimilarIncidents() {
            double score = service.computeScore(
                    0.75, "cpu_saturation", false, false, 3);

            assertTrue(score > 0.75, "Score should be boosted for similar incidents");
        }

        @Test
        @DisplayName("caps score at 1.0")
        void capsScoreAtOne() {
            double score = service.computeScore(
                    0.98, "memory_exhaustion", true, true, 5);

            assertEquals(1.0, score, 0.01);
        }

        @Test
        @DisplayName("floors score at 0.0")
        void floorsScoreAtZero() {
            double score = service.computeScore(
                    0.05, "network_issue", false, false, 0);

            assertEquals(0.0, score, 0.01);
        }
    }

    @Nested
    @DisplayName("recordResolution")
    class RecordResolution {

        @Test
        @DisplayName("updates resolution rate for successful resolution")
        void updatesRateForSuccess() {
            service.recordResolution("new_root_cause", true);
            double rate = service.getResolutionRate("new_root_cause");

            assertTrue(rate > 0.5, "Resolution rate should increase for success");
        }

        @Test
        @DisplayName("updates resolution rate for failed resolution")
        void updatesRateForFailure() {
            service.recordResolution("another_cause", false);
            double rate = service.getResolutionRate("another_cause");

            assertTrue(rate < 0.5, "Resolution rate should decrease for failure");
        }
    }
}