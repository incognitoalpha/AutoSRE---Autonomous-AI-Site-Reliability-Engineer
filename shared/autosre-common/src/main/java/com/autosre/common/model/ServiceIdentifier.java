package com.autosre.common.model;

/**
 * Immutable record representing a service identifier for tracking and routing.
 *
 * <p>Bounded context: {@code autosre-common}</p>
 *
 * @param namespace the Kubernetes namespace or deployment environment
 * @param serviceName the name of the service
 * @param clusterId the cluster identifier where the service runs
 */
public record ServiceIdentifier(
    String namespace,
    String serviceName,
    String clusterId
) {
    public ServiceIdentifier {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be null or blank");
        }
        if (serviceName == null || serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be null or blank");
        }
        if (clusterId == null || clusterId.isBlank()) {
            clusterId = "default";
        }
    }
}