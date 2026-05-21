package com.autosre.healing.executor;

import com.autosre.common.exception.AgentExecutionException;
import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KubernetesScaleExecutorTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private KubernetesScaleExecutor executor;

    // Intermediate mocks for fabric8 client
    private io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL apps;
    private io.fabric8.kubernetes.client.dsl.MixedOperation<Deployment, io.fabric8.kubernetes.api.model.apps.DeploymentList, io.fabric8.kubernetes.client.dsl.RollableScalableResource<Deployment>> deployments;
    private io.fabric8.kubernetes.client.dsl.NonNamespaceOperation<Deployment, io.fabric8.kubernetes.api.model.apps.DeploymentList, io.fabric8.kubernetes.client.dsl.RollableScalableResource<Deployment>> inNamespace;
    private io.fabric8.kubernetes.client.dsl.RollableScalableResource<Deployment> withName;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        apps = mock(io.fabric8.kubernetes.client.dsl.AppsAPIGroupDSL.class);
        deployments = mock(io.fabric8.kubernetes.client.dsl.MixedOperation.class);
        inNamespace = mock(io.fabric8.kubernetes.client.dsl.NonNamespaceOperation.class);
        withName = mock(io.fabric8.kubernetes.client.dsl.RollableScalableResource.class);

        // Setup the chain
        when(kubernetesClient.apps()).thenReturn(apps);
        when(apps.deployments()).thenReturn(deployments);
        when(deployments.inNamespace(anyString())).thenReturn(inNamespace);
        when(inNamespace.withName(anyString())).thenReturn(withName);

        executor = new KubernetesScaleExecutor(kubernetesClient);
        ReflectionTestUtils.setField(executor, "maxScaleFactor", 3.0);
    }

    @Test
    @DisplayName("Returns correct action type")
    void returnsCorrectActionType() {
        assertEquals("SCALE_DEPLOYMENT", executor.actionType());
    }

    @Test
    @DisplayName("Handles invalid replicas parameter - throws exception")
    void handlesInvalidReplicasParameter() {
        Map<String, String> params = new HashMap<>();
        params.put("replicas", "invalid");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        assertThrows(AgentExecutionException.class, () -> executor.execute(action));
    }

    @Test
    @DisplayName("Handles missing deployment - returns failure outcome")
    void handlesDeploymentNotFound() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        params.put("replicas", "3");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        when(withName.get()).thenReturn(null);

        assertThrows(AgentExecutionException.class, () -> {
            executor.execute(action);
        });
    }

    @Test
    @DisplayName("Handles already at target replicas - skips scale and returns success")
    void handlesAlreadyAtTargetReplicas() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("replicas", "2");
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        Deployment mockDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(mockDeployment.getSpec().getReplicas()).thenReturn(2);

        when(withName.get()).thenReturn(mockDeployment);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertTrue(outcome.success());
        assertEquals(2.0, outcome.postMetricValue());

        verify(withName, never()).scale(anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("Handles scale factor exceeded - caps target replicas")
    void handlesScaleFactorExceeded() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("replicas", "10"); // Requested 10
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        Deployment initialDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(initialDeployment.getSpec().getReplicas()).thenReturn(2); // Current 2, max is 6

        Deployment scaledDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(scaledDeployment.getStatus().getReadyReplicas()).thenReturn(6);

        when(withName.get())
                .thenReturn(initialDeployment, scaledDeployment);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertTrue(outcome.success());
        assertEquals(2.0, outcome.preMetricValue());
        assertEquals(6.0, outcome.postMetricValue());

        verify(withName).scale(6, true);
    }

    @Test
    @DisplayName("Handles successful scale operation")
    void handlesSuccessfulScale() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("replicas", "3");
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        Deployment initialDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(initialDeployment.getSpec().getReplicas()).thenReturn(1);

        Deployment scaledDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(scaledDeployment.getStatus().getReadyReplicas()).thenReturn(3);

        when(withName.get())
                .thenReturn(initialDeployment, scaledDeployment);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertTrue(outcome.success());
        assertEquals(1.0, outcome.preMetricValue());
        assertEquals(3.0, outcome.postMetricValue());

        verify(withName).scale(3, true);
    }

    @Test
    @DisplayName("Handles timeout during wait - returns failure outcome")
    void handlesScaleTimeout() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("replicas", "3");
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("SCALE_DEPLOYMENT", "test-dep", params);

        Deployment initialDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(initialDeployment.getSpec().getReplicas()).thenReturn(1);

        when(withName.get())
                .thenReturn(initialDeployment);

        Thread.currentThread().interrupt();
        try {
            assertThrows(AgentExecutionException.class, () -> {
                executor.execute(action);
            });
        } finally {
            Thread.interrupted(); // clear status
        }
    }
}
