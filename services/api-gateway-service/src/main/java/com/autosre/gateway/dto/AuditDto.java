package com.autosre.gateway.dto;

import java.time.Instant;

/**
 * Data transfer object for audit log responses.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 *
 * @param id           unique entry identifier
 * @param planId       the remediation plan ID
 * @param actionType    type of action (SCALE_DEPLOYMENT, RESTART_POD, etc.)
 * @param target       resource target
 * @param executor     which executor performed the action
 * @param outcome      SUCCESS, FAILURE, or ERROR
 * @param durationMs   execution duration in milliseconds
 * @param errorMessage error message if outcome is not SUCCESS
 * @param executedAt   when the action was executed
 */
public record AuditDto(
        Long id,
        String planId,
        String actionType,
        String target,
        String executor,
        String outcome,
        long durationMs,
        String errorMessage,
        Instant executedAt
) { }