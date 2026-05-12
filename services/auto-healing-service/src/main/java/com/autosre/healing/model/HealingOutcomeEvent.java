package com.autosre.healing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the outcome of a healing action execution.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 *
 * @param planId           the remediation plan that was executed
 * @param actionType       the type of action performed (SCALE_DEPLOYMENT, RESTART_POD, etc.)
 * @param target           the resource target (e.g., deployment name or pod name)
 * @param success          true if the action completed successfully
 * @param preMetricValue   the metric value before the action was taken
 * @param postMetricValue  the metric value after the action (sampled after execution)
 * @param executedAt       the timestamp when the action was executed
 * @param durationMs       how long the execution took in milliseconds
 * @param errorMessage     error message if the action failed
 */
public record HealingOutcomeEvent(
        @JsonProperty("planId") String planId,
        @JsonProperty("actionType") String actionType,
        @JsonProperty("target") String target,
        @JsonProperty("success") boolean success,
        @JsonProperty("preMetricValue") Double preMetricValue,
        @JsonProperty("postMetricValue") Double postMetricValue,
        @JsonProperty("executedAt") long executedAt,
        @JsonProperty("durationMs") long durationMs,
        @JsonProperty("errorMessage") String errorMessage
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String planId;
        private String actionType;
        private String target;
        private boolean success;
        private Double preMetricValue;
        private Double postMetricValue;
        private long executedAt;
        private long durationMs;
        private String errorMessage;

        public Builder planId(String planId) { this.planId = planId; return this; }
        public Builder actionType(String actionType) { this.actionType = actionType; return this; }
        public Builder target(String target) { this.target = target; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder preMetricValue(Double preMetricValue) { this.preMetricValue = preMetricValue; return this; }
        public Builder postMetricValue(Double postMetricValue) { this.postMetricValue = postMetricValue; return this; }
        public Builder executedAt(long executedAt) { this.executedAt = executedAt; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }

        public HealingOutcomeEvent build() {
            return new HealingOutcomeEvent(planId, actionType, target, success,
                    preMetricValue, postMetricValue, executedAt, durationMs, errorMessage);
        }
    }
}