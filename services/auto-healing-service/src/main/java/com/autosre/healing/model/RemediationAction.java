package com.autosre.healing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a remediation action to be executed by the healing service.
 * Consumed from the {@code autosre.actions.remediation} Kafka topic.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
public record RemediationAction(
        @JsonProperty("planId") String planId,
        @JsonProperty("agentId") String agentId,
        @JsonProperty("actions") java.util.List<Action> actions,
        @JsonProperty("approvalTier") String approvalTier,
        @JsonProperty("confidenceScore") double confidenceScore,
        @JsonProperty("approvedAt") long approvedAt,
        @JsonProperty("approvedBy") String approvedBy
) {
    public record Action(
            @JsonProperty("type") String type,
            @JsonProperty("target") String target,
            @JsonProperty("parameters") java.util.Map<String, String> parameters
    ) { }
}