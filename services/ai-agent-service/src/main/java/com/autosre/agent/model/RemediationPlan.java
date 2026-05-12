package com.autosre.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a concrete remediation plan produced by a specialist agent.
 * Contains ordered actions, risk classification, and impact estimates
 * to be routed through the appropriate approval gate.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @param planId unique identifier for this plan
 * @param agentId the specialist agent that produced this plan (e.g., "scaling", "recovery")
 * @param alertId the triggering anomaly alert ID
 * @param serviceId the affected service identifier
 * @param actions ordered list of remediation actions to execute
 * @param riskLevel risk classification determining approval gate routing
 * @param estimatedImpact human-readable description of expected service improvement
 * @param createdAt when this plan was created
 */
public record RemediationPlan(
        UUID planId,
        String agentId,
        String alertId,
        String serviceId,
        List<RemediationAction> actions,
        RiskLevel riskLevel,
        String estimatedImpact,
        Instant createdAt
) {

    /**
     * Represents a single step within a remediation plan.
     *
     * <p>Bounded context: {@code ai-agent-service}</p>
     *
     * @param actionType the type of action to perform
     * @param target the Kubernetes resource or service identifier targeted
     * @param parameters additional parameters for the action (e.g., replica count, image tag)
     * @param reason human-readable justification for this specific action
     */
    public record RemediationAction(
            ActionType actionType,
            String target,
            List<Parameter> parameters,
            String reason
    ) {

        /**
         * Key-value parameter for a remediation action.
         *
         * @param name parameter name
         * @param value parameter value (serialized as string)
         */
        public record Parameter(String name, String value) { }

        /**
         * Validates that actionType is not null.
         */
        public RemediationAction {
            if (actionType == null) {
                throw new IllegalArgumentException("actionType must not be null");
            }
            parameters = (parameters != null) ? List.copyOf(parameters) : List.of();
            reason = (reason != null) ? reason.trim() : "";
            target = (target != null) ? target.trim() : "";
        }
    }

    /**
     * Enumeration of supported remediation action types.
     */
    public enum ActionType {
        SCALE_DEPLOYMENT,
        SCALE_BROKERS,
        RESTART_POD,
        ROLLBACK_DEPLOYMENT,
        QUARANTINE_POD,
        ROTATE_SECRETS,
        ADJUST_RESOURCE_LIMITS,
        DRAIN_NODE,
        NOTIFY_ONCALL
    }

    /**
     * Factory method to create a new plan with a generated UUID.
     *
     * @param agentId the specialist agent ID
     * @param alertId the triggering alert ID
     * @param serviceId the affected service
     * @param actions the remediation actions
     * @param riskLevel the risk classification
     * @param estimatedImpact description of expected improvement
     * @return a new RemediationPlan with a generated UUID and current timestamp
     */
    public static RemediationPlan create(
            String agentId,
            String alertId,
            String serviceId,
            List<RemediationAction> actions,
            RiskLevel riskLevel,
            String estimatedImpact) {
        return new RemediationPlan(
                UUID.randomUUID(),
                agentId,
                alertId,
                serviceId,
                actions,
                riskLevel,
                estimatedImpact,
                Instant.now()
        );
    }

    /**
     * Validates and normalizes this plan.
     */
    public RemediationPlan {
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel must not be null");
        }
        actions = (actions != null) ? List.copyOf(actions) : List.of();
        planId = (planId != null) ? planId : UUID.randomUUID();
        createdAt = (createdAt != null) ? createdAt : Instant.now();
    }

    /**
     * Returns true if this plan is low-risk and suitable for auto-approval.
     *
     * @return true when riskLevel is LOW
     */
    public boolean isAutoApprovable() {
        return riskLevel == RiskLevel.LOW;
    }
}