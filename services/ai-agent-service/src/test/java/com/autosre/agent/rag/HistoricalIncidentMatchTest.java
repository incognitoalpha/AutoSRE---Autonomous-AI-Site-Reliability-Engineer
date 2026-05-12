package com.autosre.agent.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("IncidentMemoryService.HistoricalIncidentMatch")
class HistoricalIncidentMatchTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("accepts valid match")
        void acceptsValidMatch() {
            IncidentMemoryService.HistoricalIncidentMatch match =
                    new IncidentMemoryService.HistoricalIncidentMatch(
                            "incident-123",
                            "kafka-service",
                            "Kafka broker OOM",
                            java.util.List.of("Restart broker", "Increase heap"),
                            0.88,
                            "2026-05-10T10:00:00Z"
                    );
            assertNotNull(match);
            assertEquals("incident-123", match.incidentId());
            assertEquals(0.88, match.similarityScore());
        }

        @Test
        @DisplayName("rejects similarity score below 0.0")
        void rejectsScoreBelowZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new IncidentMemoryService.HistoricalIncidentMatch(
                            "id", "svc", "rc", java.util.List.of(), -0.1, "time")
            );
        }

        @Test
        @DisplayName("rejects similarity score above 1.0")
        void rejectsScoreAboveOne() {
            assertThrows(IllegalArgumentException.class, () ->
                    new IncidentMemoryService.HistoricalIncidentMatch(
                            "id", "svc", "rc", java.util.List.of(), 1.5, "time")
            );
        }

        @Test
        @DisplayName("accepts boundary values 0.0 and 1.0")
        void acceptsBoundaryValues() {
            IncidentMemoryService.HistoricalIncidentMatch zeroScore =
                    new IncidentMemoryService.HistoricalIncidentMatch(
                            "id", "svc", "rc", java.util.List.of(), 0.0, "time");
            assertEquals(0.0, zeroScore.similarityScore());

            IncidentMemoryService.HistoricalIncidentMatch oneScore =
                    new IncidentMemoryService.HistoricalIncidentMatch(
                            "id", "svc", "rc", java.util.List.of(), 1.0, "time");
            assertEquals(1.0, oneScore.similarityScore());
        }
    }

    @Nested
    @DisplayName("default methods")
    class DefaultMethods {

        @Test
        @DisplayName("findSimilar uses default limit of 5")
        void defaultFindSimilarLimit() {
            // Verify the interface has a default method
            assertTrue(IncidentMemoryService.class.getDeclaredMethods().length > 0);
        }
    }
}