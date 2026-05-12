package com.autosre.healing.executor;

import com.autosre.common.exception.AgentExecutionException;
import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Scales Kubernetes Deployments to a target replica count.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Component
public final class KubernetesScaleExecutor implements HealingActionExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesScaleExecutor.class);
    private static final double MAX_SCALE_FACTOR = 3.0;

    private final KubernetesClient kubernetesClient;

    @Value("${autosre.kubernetes.scale.max-factor:3.0}")
    private double maxScaleFactor = MAX_SCALE_FACTOR;

    public KubernetesScaleExecutor(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public String actionType() {
        return "SCALE_DEPLOYMENT";
    }

    @Override
    public HealingOutcomeEvent execute(RemediationAction.Action action) throws AgentExecutionException {
        long startTime = System.currentTimeMillis();
        String targetDeployment = action.target();
        String namespace = action.parameters().getOrDefault("namespace", "default");
        int targetReplicas;

        try {
            String replicasStr = action.parameters().getOrDefault("replicas", "1");
            targetReplicas = Integer.parseInt(replicasStr);
        } catch (NumberFormatException e) {
            throw new AgentExecutionException("scale-executor", "Invalid replicas parameter: " + action.parameters().get("replicas"), e);
        }

        LOG.info("Executing SCALE action: deployment={}, namespace={}, replicas={}",
                targetDeployment, namespace, targetReplicas);

        try {
            var deployment = kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(targetDeployment)
                    .get();

            if (deployment == null) {
                throw new AgentExecutionException("Deployment not found: " + targetDeployment + " in namespace: " + namespace);
            }

            int currentReplicas = deployment.getSpec().getReplicas();
            int maxAllowedReplicas = currentReplicas == 0
                    ? (int) Math.round(maxScaleFactor)
                    : (int) Math.round(currentReplicas * maxScaleFactor);

            if (currentReplicas == 0 && targetReplicas > maxAllowedReplicas) {
                LOG.warn("Scale factor exceeded when scaling from zero: requested={}, maxAllowed={}",
                        targetReplicas, maxAllowedReplicas);
                targetReplicas = maxAllowedReplicas;
            } else if (currentReplicas > 0 && targetReplicas > maxAllowedReplicas) {
                LOG.warn("Scale factor exceeded: requested={}, maxAllowed={}, current={}",
                        targetReplicas, maxAllowedReplicas, currentReplicas);
                targetReplicas = maxAllowedReplicas;
            }

            if (currentReplicas == targetReplicas) {
                LOG.info("Deployment already at target replica count: {}", currentReplicas);
                return buildSuccessOutcome(action, (double) currentReplicas, (double) targetReplicas, startTime);
            }

            kubernetesClient.apps().deployments()
                    .inNamespace(namespace)
                    .withName(targetDeployment)
                    .scale(targetReplicas, true);

            boolean scaled = waitForReplicas(namespace, targetDeployment, targetReplicas);
            if (!scaled) {
                throw new AgentExecutionException("Scale operation timed out: " + targetDeployment);
            }

            double preMetric = (double) currentReplicas;
            double postMetric = (double) targetReplicas;
            LOG.info("Scale successful: {} -> {} replicas", currentReplicas, targetReplicas);

            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(targetDeployment)
                    .success(true)
                    .preMetricValue(preMetric)
                    .postMetricValue(postMetric)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Scale operation failed: deployment={}", targetDeployment, e);
            return HealingOutcomeEvent.builder()
                    .planId(action.parameters().getOrDefault("planId", "unknown"))
                    .actionType(actionType())
                    .target(targetDeployment)
                    .success(false)
                    .executedAt(Instant.now().toEpochMilli())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private boolean waitForReplicas(String namespace, String deployment, int targetReplicas) {
        int maxWaitSeconds = 120;
        int waited = 0;
        while (waited < maxWaitSeconds) {
            try {
                Thread.sleep(2000);
                waited += 2;

                var status = kubernetesClient.apps().deployments()
                        .inNamespace(namespace)
                        .withName(deployment)
                        .get()
                        .getStatus();

                int readyReplicas = status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
                if (readyReplicas >= targetReplicas) {
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private HealingOutcomeEvent buildSuccessOutcome(RemediationAction.Action action, double preMetric, double postMetric, long startTime) {
        return HealingOutcomeEvent.builder()
                .planId(action.parameters().getOrDefault("planId", "unknown"))
                .actionType(actionType())
                .target(action.target())
                .success(true)
                .preMetricValue(preMetric)
                .postMetricValue(postMetric)
                .executedAt(Instant.now().toEpochMilli())
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
    }
}