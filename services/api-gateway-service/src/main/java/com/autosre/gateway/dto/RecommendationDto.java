package com.autosre.gateway.dto;

import java.util.List;

/**
 * Data transfer object for remediation recommendation responses.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 *
 * @param planId          unique plan identifier
 * @param agentId         which agent produced this recommendation
 * @param actions         list of remediation actions
 * @param approvalTier    AUTO, ASYNC, or SYNC
 * @param confidenceScore confidence score (0.0-1.0)
 * @param approvedAt      when the recommendation was approved
 * @param approvedBy      who/what approved (auto-system or user)
 */
public record RecommendationDto(
        String planId,
        String agentId,
        List<ActionDto> actions,
        String approvalTier,
        double confidenceScore,
        long approvedAt,
        String approvedBy
) {
    public record ActionDto(
            String type,
            String target,
            String parameters
    ) { }
}