package com.autosre.healing.executor;

import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.api.model.Pod;
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
class PodRestartExecutorTest {

    @Mock
    private KubernetesClient kubernetesClient;

    private PodRestartExecutor executor;

    private io.fabric8.kubernetes.client.dsl.MixedOperation<Pod, io.fabric8.kubernetes.api.model.PodList, io.fabric8.kubernetes.client.dsl.PodResource> pods;
    private io.fabric8.kubernetes.client.dsl.NonNamespaceOperation<Pod, io.fabric8.kubernetes.api.model.PodList, io.fabric8.kubernetes.client.dsl.PodResource> inNamespace;
    private io.fabric8.kubernetes.client.dsl.PodResource withName;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pods = mock(io.fabric8.kubernetes.client.dsl.MixedOperation.class);
        inNamespace = mock(io.fabric8.kubernetes.client.dsl.NonNamespaceOperation.class);
        withName = mock(io.fabric8.kubernetes.client.dsl.PodResource.class);

        when(kubernetesClient.pods()).thenReturn(pods);
        when(pods.inNamespace(anyString())).thenReturn(inNamespace);
        when(inNamespace.withName(anyString())).thenReturn(withName);

        executor = new PodRestartExecutor(kubernetesClient);
    }

    @Test
    @DisplayName("Returns correct action type")
    void returnsCorrectActionType() {
        assertEquals("RESTART_POD", executor.actionType());
    }

    @Test
    @DisplayName("Handles missing pod - returns failure outcome")
    void handlesPodNotFound() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("RESTART_POD", "test-pod", params);

        when(withName.get()).thenReturn(null);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertFalse(outcome.success());
        assertTrue(outcome.errorMessage().contains("Pod not found"));
    }

    @Test
    @DisplayName("Handles KubernetesClientException during delete - returns failure outcome")
    void handlesDeleteException() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("RESTART_POD", "test-pod", params);

        Pod mockPod = mock(Pod.class, Answers.RETURNS_DEEP_STUBS);
        when(mockPod.getMetadata().getUid()).thenReturn("old-uid");

        when(withName.get()).thenReturn(mockPod);
        when(withName.delete()).thenThrow(new KubernetesClientException("Delete failed"));

        HealingOutcomeEvent outcome = executor.execute(action);
        assertFalse(outcome.success());
        assertTrue(outcome.errorMessage().contains("Delete failed"));
    }

    @Test
    @DisplayName("Handles successful pod restart")
    void handlesSuccessfulRestart() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("RESTART_POD", "test-pod", params);

        Pod oldPod = mock(Pod.class, Answers.RETURNS_DEEP_STUBS);
        when(oldPod.getMetadata().getUid()).thenReturn("old-uid");

        Pod newPod = mock(Pod.class, Answers.RETURNS_DEEP_STUBS);
        when(newPod.getMetadata().getUid()).thenReturn("new-uid");
        when(newPod.getStatus().getPhase()).thenReturn("Running");

        when(withName.get()).thenReturn(oldPod, newPod);

        HealingOutcomeEvent outcome = executor.execute(action);
        assertTrue(outcome.success());
        assertEquals("test-pod", outcome.target());

        verify(withName).delete();
    }

    @Test
    @DisplayName("Handles pod restart timeout (new pod not running) - returns failure outcome")
    void handlesRestartTimeout() {
        Map<String, String> params = new HashMap<>();
        params.put("namespace", "default");
        RemediationAction.Action action = new RemediationAction.Action("RESTART_POD", "test-pod", params);

        Pod oldPod = mock(Pod.class, Answers.RETURNS_DEEP_STUBS);
        when(oldPod.getMetadata().getUid()).thenReturn("old-uid");

        // The pod never changes uid in this mocked scenario
        when(withName.get()).thenReturn(oldPod);

        Thread.currentThread().interrupt();
        try {
            HealingOutcomeEvent outcome = executor.execute(action);
            assertFalse(outcome.success());
            assertTrue(outcome.errorMessage().contains("Pod did not reach Running state"));
        } finally {
            Thread.interrupted(); // clear status
        }
    }
}
