package com.autosre.healing.executor;

import com.autosre.common.exception.AgentExecutionException;
import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Deletes target pods, allowing Kubernetes to recreate them.
 * Monitors the pod until it reaches Running state within 120 seconds.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Component
public final class PodRestartExecutor implements HealingActionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PodRestartExecutor.class);
    private static final int MAX_WAIT_SECONDS = 120;

    private final KubernetesClient kubernetesClient;

    public PodRestartExecutor(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public String actionType() {
        return "RESTART_POD";
    }

    @Override
    public HealingOutcomeEvent execute(RemediationAction.Action action) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        String podName = action.target();
        String namespace = action.parameters().getOrDefault("namespace", "default");

        LOG.info("Executing RESTART_POD action: pod={}, namespace={}", podName, namespace);

        try {
            var pod = kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .get();

            if (pod == null) {
                throw new AgentExecutionException("Pod not found: " + podName + " in namespace: " + namespace);
            }

            String uidBefore = pod.getMetadata().getUid();
            LOG.info("Deleting pod: {} (uid={})", podName, uidBefore);

            kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .delete();

            boolean podRunning = waitForPodRunning(namespace, podName, uidBefore);

            if (!podRunning) {
                throw new AgentExecutionException("Pod did not reach Running state within " + MAX_WAIT_SECONDS + "s: " + podName);
            }

            LOG.info("Pod restarted successfully: {}", podName);
            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(podName)
                    .success(true)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (Exception e) {
            LOG.error("Pod restart failed: pod={}", podName, e);
            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(podName)
                    .success(false)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private boolean waitForPodRunning(String namespace, String podName, String oldUid) {
        int waited = 0;
        while (waited < MAX_WAIT_SECONDS) {
            try {
                Thread.sleep(3000);
                waited += 3;

                var pod = kubernetesClient.pods()
                        .inNamespace(namespace)
                        .withName(podName)
                        .get();

                if (pod != null && !pod.getMetadata().getUid().equals(oldUid)) {
                    String phase = pod.getStatus().getPhase();
                    if ("Running".equals(phase)) {
                        return true;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}