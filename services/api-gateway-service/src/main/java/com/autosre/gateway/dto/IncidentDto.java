package com.autosre.gateway.dto;

import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.UUID;

/**
 * Data transfer object for incident responses.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 *
 * @param id          unique incident identifier
 * @param serviceId   the affected service name
 * @param severity    incident severity level
 * @param rootCause   root cause analysis summary
 * @param status      incident status (OPEN, INVESTIGATING, RESOLVED)
 * @param detectedAt  when the incident was first detected
 * @param resolvedAt  when the incident was resolved (null if still open)
 */
public record IncidentDto(
        UUID id,
        String serviceId,
        Severity severity,
        String rootCause,
        String status,
        Instant detectedAt,
        Instant resolvedAt
) {
    public enum Status {
        OPEN, INVESTIGATING, RESOLVED
    }
}