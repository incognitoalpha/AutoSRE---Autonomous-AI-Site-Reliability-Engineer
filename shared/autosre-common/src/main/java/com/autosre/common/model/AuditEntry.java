package com.autosre.common.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record representing an audit log entry for tracking all agent actions.
 *
 * <p>Bounded context: {@code autosre-common}</p>
 *
 * @param id unique identifier for this audit entry
 * @param actionType the type of action taken (e.g., SCALE_DEPLOYMENT, RESTART_POD)
 * @param targetService the service identifier targeted by this action
 * @param agentId the identifier of the agent that initiated this action
 * @param outcome the outcome of the action (SUCCESS, FAILURE, PARTIAL)
 * @param executedAt the timestamp when the action was executed
 * @param durationMs the duration of the action in milliseconds
 * @param details additional details or error messages
 */
public record AuditEntry(
    UUID id,
    String actionType,
    String targetService,
    String agentId,
    Outcome outcome,
    Instant executedAt,
    long durationMs,
    String details
) {
    public enum Outcome {
        SUCCESS,
        FAILURE,
        PARTIAL
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String actionType;
        private String targetService;
        private String agentId;
        private Outcome outcome;
        private Instant executedAt = Instant.now();
        private long durationMs;
        private String details;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder targetService(String targetService) {
            this.targetService = targetService;
            return this;
        }

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder outcome(Outcome outcome) {
            this.outcome = outcome;
            return this;
        }

        public Builder executedAt(Instant executedAt) {
            this.executedAt = executedAt;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public AuditEntry build() {
            return new AuditEntry(id, actionType, targetService, agentId, outcome,
                                  executedAt, durationMs, details);
        }
    }
}