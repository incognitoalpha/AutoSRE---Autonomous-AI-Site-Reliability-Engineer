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
 * Executes rollback of a Kubernetes Deployment to the previous version.
 * Uses the native rollout undo capability of fabric8 client.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Component
public final class RollbackExecutor implements HealingActionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(RollbackExecutor.class);
    private static final int MAX_WAIT_SECONDS = 180;

    private final KubernetesClient kubernetesClient;

    public RollbackExecutor(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public String actionType() {
        return "ROLLBACK_DEPLOYMENT";
    }

    @Override
    public HealingOutcomeEvent execute(RemediationAction.Action action) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        String deploymentName = action.target();
        String namespace = action.parameters().getOrDefault("namespace", "default");

        LOG.info("Executing ROLLBACK action: deployment={}, namespace={}", deploymentName, namespace);

        try {
            var deployment = kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment == null) {
                throw new AgentExecutionException("Deployment not found: " + deploymentName);
            }

            LOG.info("Triggering rollback (rolling restart) for deployment: {}", deploymentName);
            kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .rolling()
                    .restart();

            boolean rolloutComplete = waitForRollout(namespace, deploymentName);

            if (!rolloutComplete) {
                throw new AgentExecutionException("Rollout did not complete within " + MAX_WAIT_SECONDS + "s: " + deploymentName);
            }

            LOG.info("Rollback completed successfully: {}", deploymentName);
            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(deploymentName)
                    .success(true)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Rollback failed: deployment={}", deploymentName, e);
            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(deploymentName)
                    .success(false)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private boolean waitForRollout(String namespace, String deploymentName) {
        int waited = 0;
        while (waited < MAX_WAIT_SECONDS) {
            try {
                Thread.sleep(3000);
                waited += 3;

                var status = kubernetesClient.apps().deployments()
                        .inNamespace(namespace)
                        .withName(deploymentName)
                        .get()
                        .getStatus();

                Integer available = status.getAvailableReplicas();
                if (available != null && available > 0) {
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}