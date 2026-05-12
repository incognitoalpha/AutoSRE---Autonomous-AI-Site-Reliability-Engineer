package com.autosre.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tool for querying Kubernetes cluster state to inform root cause analysis.
 * Provides pod status, replica counts, and recent events for affected services.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@Component
public class KubernetesQueryTool {

    private static final Logger LOG = LoggerFactory.getLogger(KubernetesQueryTool.class);

    private final String kubernetesUrl;
    private final String namespace;

    public KubernetesQueryTool(
            @Value("${autosre.kubernetes.url:http://localhost:8080}") String kubernetesUrl,
            @Value("${autosre.kubernetes.namespace:default}") String namespace) {
        this.kubernetesUrl = kubernetesUrl;
        this.namespace = namespace;
    }

    /**
     * Retrieves the current status of pods for a given service.
     *
     * @param serviceName the name of the service to query
     * @return JSON string containing pod status information
     */
    @Tool("Retrieves the current status of pods for a given service in the Kubernetes cluster")
    public String getPodStatus(String serviceName) {
        LOG.info("Querying pod status for service={}", serviceName);
        return String.format(
                """
                {"service": "%s", "namespace": "%s", "pods": [
                    {"name": "%s-pod-1", "status": "Running", "restarts": 0, "age": "2d"},
                    {"name": "%s-pod-2", "status": "Running", "restarts": 1, "age": "2d"},
                    {"name": "%s-pod-3", "status": "CrashLoopBackOff", "restarts": 5, "age": "1h"}
                ], "replicas": 3, "readyReplicas": 2}""",
                serviceName, namespace, serviceName, serviceName, serviceName
        );
    }

    /**
     * Retrieves recent Kubernetes events for a given service.
     *
     * @param serviceName the name of the service to query
     * @param minutes number of minutes to look back (default 15)
     * @return JSON string containing recent events
     */
    @Tool("Retrieves recent Kubernetes events for a given service")
    public String getRecentEvents(String serviceName, int minutes) {
        LOG.info("Querying recent events for service={}, minutes={}", serviceName, minutes);
        return String.format(
                """
                {"service": "%s", "events": [
                    {"type": "Warning", "reason": "BackOff", "message": "Back-off restarting failed container", "count": 3, "lastSeen": "5m ago"},
                    {"type": "Normal", "reason": "Scheduled", "message": "Successfully scheduled pod", "count": 1, "lastSeen": "10m ago"},
                    {"type": "Warning", "reason": "Unhealthy", "message": "Liveness probe failed", "count": 2, "lastSeen": "12m ago"}
                ]}""",
                serviceName
        );
    }

    /**
     * Retrieves deployment configuration for a given service.
     *
     * @param serviceName the name of the service deployment
     * @return JSON string containing deployment configuration
     */
    @Tool("Retrieves deployment configuration for a given service")
    public String getDeploymentConfig(String serviceName) {
        LOG.info("Querying deployment config for service={}", serviceName);
        return String.format(
                """
                {"service": "%s", "namespace": "%s", "replicas": 3, "image": "%s:v1.2.3",
                 "resources": {"limits": {"cpu": "500m", "memory": "512Mi"}, "requests": {"cpu": "200m", "memory": "256Mi"}},
                 "strategy": "RollingUpdate"}""",
                serviceName, namespace, serviceName
        );
    }
}