package com.autosre.agent.model;

import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RootCauseAnalysis")
class RootCauseAnalysisTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("accepts valid confidence score 0.5")
        void acceptsValidConfidenceScore() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "Kafka broker OOM",
                    List.of("kafka-broker-0", "kafka-broker-1"),
                    0.5,
                    List.of("Increase heap", "Add brokers"),
                    "Memory utilization 95%",
                    "alert-123",
                    "kafka-service",
                    Severity.HIGH,
                    Instant.now()
            );
            assertNotNull(rca);
            assertEquals(0.5, rca.confidenceScore());
        }

        @Test
        @DisplayName("rejects confidence score above 1.0")
        void rejectsConfidenceScoreAboveOne() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RootCauseAnalysis(
                            "root cause",
                            List.of(),
                            1.5,
                            List.of(),
                            "evidence",
                            "alert-1",
                            "svc",
                            Severity.MEDIUM,
                            Instant.now()
                    )
            );
        }

        @Test
        @DisplayName("rejects confidence score below 0.0")
        void rejectsConfidenceScoreBelowZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new RootCauseAnalysis(
                            "root cause",
                            List.of(),
                            -0.1,
                            List.of(),
                            "evidence",
                            "alert-1",
                            "svc",
                            Severity.LOW,
                            Instant.now()
                    )
            );
        }

        @Test
        @DisplayName("accepts confidence score 0.0")
        void acceptsConfidenceScoreZero() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "unknown",
                    List.of(),
                    0.0,
                    List.of(),
                    "no evidence",
                    "alert-1",
                    "svc",
                    Severity.LOW,
                    Instant.now()
            );
            assertEquals(0.0, rca.confidenceScore());
        }

        @Test
        @DisplayName("accepts confidence score 1.0")
        void acceptsConfidenceScoreOne() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "confirmed",
                    List.of("svc-a"),
                    1.0,
                    List.of("action1"),
                    "strong evidence",
                    "alert-1",
                    "svc",
                    Severity.CRITICAL,
                    Instant.now()
            );
            assertEquals(1.0, rca.confidenceScore());
        }
    }

    @Nested
    @DisplayName("isHighConfidence")
    class IsHighConfidence {

        @Test
        @DisplayName("returns true when score >= 0.90")
        void returnsTrueWhenHigh() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc", List.of(), 0.90, List.of(), "", "a", "s", Severity.MEDIUM, Instant.now());
            assertTrue(rca.isHighConfidence());
        }

        @Test
        @DisplayName("returns false when score < 0.90")
        void returnsFalseWhenLow() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc", List.of(), 0.89, List.of(), "", "a", "s", Severity.MEDIUM, Instant.now());
            assertFalse(rca.isHighConfidence());
        }
    }

    @Nested
    @DisplayName("isActionable")
    class IsActionable {

        @Test
        @DisplayName("returns true when score >= 0.75")
        void returnsTrueWhenActionable() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc", List.of(), 0.75, List.of(), "", "a", "s", Severity.MEDIUM, Instant.now());
            assertTrue(rca.isActionable());
        }

        @Test
        @DisplayName("returns false when score < 0.75")
        void returnsFalseWhenNotActionable() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc", List.of(), 0.74, List.of(), "", "a", "s", Severity.MEDIUM, Instant.now());
            assertFalse(rca.isActionable());
        }
    }

    @Nested
    @DisplayName("normalization")
    class Normalization {

        @Test
        @DisplayName("trims root cause whitespace")
        void trimsRootCause() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "  Kafka OOM  ",
                    List.of("broker-0"),
                    0.85,
                    List.of("Restart"),
                    "  evidence  ",
                    "alert-1",
                    "kafka",
                    Severity.HIGH,
                    Instant.now()
            );
            assertEquals("Kafka OOM", rca.rootCause());
            assertEquals("evidence", rca.evidenceSummary());
        }

        @Test
        @DisplayName("defaults null lists to empty immutable lists")
        void defaultsNullListsToEmpty() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc",
                    null,
                    0.8,
                    null,
                    "ev",
                    "alert-1",
                    "svc",
                    Severity.LOW,
                    Instant.now()
            );
            assertTrue(rca.affectedComponents().isEmpty());
            assertTrue(rca.recommendedActions().isEmpty());
        }

        @Test
        @DisplayName("defaults analyzedAt to now when null")
        void defaultsAnalyzedAtToNow() {
            RootCauseAnalysis rca = new RootCauseAnalysis(
                    "rc", List.of(), 0.8, List.of(), "ev", "alert-1", "svc", Severity.LOW, null);
            assertNotNull(rca.analyzedAt());
        }
    }
}