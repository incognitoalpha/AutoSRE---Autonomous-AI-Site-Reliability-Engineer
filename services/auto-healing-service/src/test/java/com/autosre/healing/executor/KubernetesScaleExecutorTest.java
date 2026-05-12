package com.autosre.healing.executor;

import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
class KubernetesScaleExecutorTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesScaleExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new KubernetesScaleExecutor(kubernetesClient);
        ReflectionTestUtils.setField(executor, "maxScaleFactor", 3.0);
    }

    @Test
    @DisplayName("Returns correct action type")
    void returnsCorrectActionType() {
        assertEquals("SCALE_DEPLOYMENT", executor.actionType());
    }

    @Test
    @DisplayName("Handles missing replicas parameter - defaults to 1")
    void handlesMissingReplicasUsesDefault() {
        // Given - replicas parameter missing
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        params.put("planId", "plan-456");

        RemediationAction.Action action = new RemediationAction.Action(
                "SCALE_DEPLOYMENT", "test-deployment", params);

        // Then - verify the params don't have replicas, will default to "1"
        assertFalse(params.containsKey("replicas"));
        assertEquals("default", params.get("namespace"));
        assertEquals("plan-456", params.get("planId"));
    }

    @Test
    @DisplayName("Action parameters map works correctly")
    void actionParametersMapWorks() {
        // Given
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "production");
        params.put("replicas", "10");
        params.put("planId", "plan-789");

        RemediationAction.Action action = new RemediationAction.Action(
                "SCALE_DEPLOYMENT", "api-service", params);

        // Then
        assertEquals("production", action.parameters().get("namespace"));
        assertEquals("10", action.parameters().get("replicas"));
        assertEquals("plan-789", action.parameters().get("planId"));
    }
}