package com.autosre.healing.executor;

import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RollbackExecutorTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private RollbackExecutor executor;

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
        withName = mock(io.fabric8.kubernetes.client.dsl.RollableScalableResource.class, Answers.RETURNS_DEEP_STUBS);

        when(kubernetesClient.apps()).thenReturn(apps);
        when(apps.deployments()).thenReturn(deployments);
        when(deployments.inNamespace(anyString())).thenReturn(inNamespace);
        when(inNamespace.withName(anyString())).thenReturn(withName);

        executor = new RollbackExecutor(kubernetesClient);
    }

    @Test
    @DisplayName("Returns correct action type")
    void returnsCorrectActionType() {
        assertEquals("ROLLBACK_DEPLOYMENT", executor.actionType());
    }

    @Test
    @DisplayName("Handles missing deployment - returns failure outcome")
    void handlesDeploymentNotFound() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("ROLLBACK_DEPLOYMENT", "test-dep", params);

        when(withName.get()).thenReturn(null);

        assertThrows(com.autosre.common.exception.AgentExecutionException.class, () -> {
            executor.execute(action);
        });
    }

    @Test
    @DisplayName("Handles KubernetesClientException during restart - returns failure outcome")
    void handlesRestartException() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("ROLLBACK_DEPLOYMENT", "test-dep", params);

        Deployment mockDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);

        when(withName.get()).thenReturn(mockDeployment);
        when(withName.rolling().restart()).thenThrow(new KubernetesClientException("Restart failed"));

        HealingOutcomeEvent outcome = executor.execute(action);
        assertFalse(outcome.success());
        assertTrue(outcome.errorMessage().contains("Restart failed"));
    }

    @Test
    @DisplayName("Handles successful rollout")
    void handlesSuccessfulRollout() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("ROLLBACK_DEPLOYMENT", "test-dep", params);

        Deployment initialDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(initialDeployment.getStatus().getAvailableReplicas()).thenReturn(0);

        Deployment rolledOutDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(rolledOutDeployment.getStatus().getAvailableReplicas()).thenReturn(1);

        // First call is initial get(), subsequent calls are in waitForRollout loop
        when(withName.get()).thenReturn(initialDeployment, rolledOutDeployment);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertTrue(outcome.success());
        assertEquals("test-dep", outcome.target());

        verify(withName.rolling()).restart();
    }

    @Test
    @DisplayName("Handles timeout during rollout - returns failure outcome")
    void handlesRolloutTimeout() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("ROLLBACK_DEPLOYMENT", "test-dep", params);

        Deployment initialDeployment = mock(Deployment.class, Answers.RETURNS_DEEP_STUBS);
        when(initialDeployment.getStatus().getAvailableReplicas()).thenReturn(0);

        when(withName.get()).thenReturn(initialDeployment);

        Thread.currentThread().interrupt();
        try {
            assertThrows(com.autosre.common.exception.AgentExecutionException.class, () -> {
                executor.execute(action);
            });
        } finally {
            Thread.interrupted(); // clear status
        }
    }
}
