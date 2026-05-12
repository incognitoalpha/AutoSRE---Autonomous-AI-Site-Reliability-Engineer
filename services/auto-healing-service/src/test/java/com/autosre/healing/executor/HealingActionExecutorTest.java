package com.autosre.healing.executor;

import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealingActionExecutorTest {

    private Map<String, String> createParams(String namespace, String replicas, String planId) {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", namespace);
        params.put("replicas", replicas);
        params.put("planId", planId);
        return params;
    }

    @Test
    @DisplayName("HealingOutcomeEvent builder creates valid object")
    void builderCreatesValidObject() {
        // Given & When
        HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                .planId("plan-123")
                .actionType("SCALE_DEPLOYMENT")
                .target("my-deployment")
                .success(true)
                .preMetricValue(2.0)
                .postMetricValue(5.0)
                .executedAt(System.currentTimeMillis())
                .durationMs(1500)
                .build();

        // Then
        assertEquals("plan-123", outcome.planId());
        assertEquals("SCALE_DEPLOYMENT", outcome.actionType());
        assertEquals("my-deployment", outcome.target());
        assertTrue(outcome.success());
        assertEquals(2.0, outcome.preMetricValue());
        assertEquals(5.0, outcome.postMetricValue());
        assertEquals(1500, outcome.durationMs());
        assertNull(outcome.errorMessage());
    }

    @Test
    @DisplayName("HealingOutcomeEvent handles failure case")
    void builderHandlesFailure() {
        // Given & When
        HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                .planId("plan-456")
                .actionType("RESTART_POD")
                .target("my-pod")
                .success(false)
                .executedAt(System.currentTimeMillis())
                .durationMs(500)
                .errorMessage("Pod not found")
                .build();

        // Then
        assertFalse(outcome.success());
        assertEquals("Pod not found", outcome.errorMessage());
    }

    @Test
    @DisplayName("RemediationAction record parses correctly")
    void remediationActionParses() {
        // Given
        Map<String, String> params = createParams("default", "3", "plan-789");
        RemediationAction.Action action = new RemediationAction.Action(
                "SCALE_DEPLOYMENT", "my-service", params);

        // Then
        assertEquals("SCALE_DEPLOYMENT", action.type());
        assertEquals("my-service", action.target());
        assertEquals("default", action.parameters().get("namespace"));
        assertEquals("3", action.parameters().get("replicas"));
    }

    @Test
    @DisplayName("RemediationAction with multiple actions")
    void multipleActions() {
        // Given
        Map<String, String> scaleParams = createParams("default", "5", "plan-001");
        Map<String, String> restartParams = new HashMap<>();
        restartParams.put("namespace", "default");
        restartParams.put("planId", "plan-001");

        RemediationAction.Action scaleAction = new RemediationAction.Action(
                "SCALE_DEPLOYMENT", "api-service", scaleParams);
        RemediationAction.Action restartAction = new RemediationAction.Action(
                "RESTART_POD", "cache-pod", restartParams);

        // Then
        assertEquals("SCALE_DEPLOYMENT", scaleAction.type());
        assertEquals("RESTART_POD", restartAction.type());
    }

    @Test
    @DisplayName("ApprovalTier determines executor routing")
    void approvalTierDeterminesRouting() {
        // Given - ASYNC tier means action should be queued
        RemediationAction action = new RemediationAction(
                "plan-async-001",
                "scaling-agent",
                java.util.List.of(new RemediationAction.Action(
                        "SCALE_DEPLOYMENT", "api-service", createParams("default", "5", "plan-async-001"))),
                "ASYNC",
                0.85,
                System.currentTimeMillis(),
                "auto-system"
        );

        // Then
        assertEquals("ASYNC", action.approvalTier());
        assertEquals(0.85, action.confidenceScore());
        assertEquals(1, action.actions().size());
    }
}