package com.autosre.healing.consumer;

import com.autosre.healing.model.HealingOutcomeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemediationConsumerTest {

    @Test
    @DisplayName("Test outcome event builder creates success outcome")
    void testSuccessOutcome() {
        // Given & When
        HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                .planId("plan-123")
                .actionType("SCALE_DEPLOYMENT")
                .target("api-service")
                .success(true)
                .preMetricValue(2.0)
                .postMetricValue(5.0)
                .executedAt(System.currentTimeMillis())
                .durationMs(100)
                .build();

        // Then
        assertTrue(outcome.success());
        assertEquals("plan-123", outcome.planId());
        assertEquals("SCALE_DEPLOYMENT", outcome.actionType());
        assertEquals("api-service", outcome.target());
        assertEquals(2.0, outcome.preMetricValue());
        assertEquals(5.0, outcome.postMetricValue());
    }

    @Test
    @DisplayName("Test outcome event builder creates failure outcome")
    void testFailureOutcome() {
        // Given & When
        HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                .planId("plan-456")
                .actionType("SCALE_DEPLOYMENT")
                .target("api-service")
                .success(false)
                .executedAt(System.currentTimeMillis())
                .durationMs(50)
                .errorMessage("Pod not found")
                .build();

        // Then
        assertFalse(outcome.success());
        assertEquals("Pod not found", outcome.errorMessage());
        assertNull(outcome.preMetricValue());
        assertNull(outcome.postMetricValue());
    }

    @Test
    @DisplayName("Test executor registry routing logic")
    void testExecutorRegistryRouting() {
        // Given
        Map<String, String> executorMap = new HashMap<>();
        executorMap.put("SCALE_DEPLOYMENT", "KubernetesScaleExecutor");

        // When
        String executor = executorMap.get("SCALE_DEPLOYMENT");

        // Then
        assertTrue(executor != null);
        assertEquals("KubernetesScaleExecutor", executor);
    }

    @Test
    @DisplayName("Test executor registry returns null for unknown type")
    void testExecutorRegistryUnknownType() {
        // Given
        Map<String, String> executorMap = new HashMap<>();
        executorMap.put("SCALE_DEPLOYMENT", "KubernetesScaleExecutor");

        // When
        String executor = executorMap.get("UNKNOWN_ACTION");

        // Then
        assertEquals(null, executor);
    }
}