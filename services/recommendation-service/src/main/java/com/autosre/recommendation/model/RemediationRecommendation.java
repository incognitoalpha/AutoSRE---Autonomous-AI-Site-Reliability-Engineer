package com.autosre.recommendation.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents an approved remediation recommendation ready for execution.
 * Contains the full plan details, approval metadata, and execution metadata.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 *
 * @param planId the unique identifier from the original RemediationPlan
 * @param agentId the specialist agent that produced the plan
 * @param alertId the triggering anomaly alert ID
 * @param serviceId the affected service identifier
 * @param actions the ordered list of remediation actions
 * @param approvalTier the tier at which this plan was approved (AUTO, ASYNC, SYNC)
 * @param riskLevel the risk classification of the plan
 * @param confidenceScore the overall confidence score after scoring adjustments
 * @param approvedAt when the plan was approved
 * @param approvedBy who/what approved the plan (system, on-call engineer, or "pending")
 * @param publishedAt when the recommendation was published to Kafka
 */
public record RemediationRecommendation(
        UUID planId,
        String agentId,
        String alertId,
        String serviceId,
        List<Action> actions,
        ApprovalTier approvalTier,
        RiskLevel riskLevel,
        double confidenceScore,
        Instant approvedAt,
        String approvedBy,
        Instant publishedAt
) {

    /**
     * Approval tier enumeration.
     */
    public enum ApprovalTier {
        /** Auto-approved by system with high confidence and low risk. */
        AUTO,
        /** Approved after async wait period unless vetoed. */
        ASYNC,
        /** Requires explicit human approval. */
        SYNC
    }

    /**
     * Risk level classification.
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    /**
     * A single action within a remediation plan.
     *
     * @param actionType the type of action
     * @param target the target resource
     * @param parameters key-value parameters
     * @param reason human-readable justification
     */
    public record Action(
            String actionType,
            String target,
            List<Parameter> parameters,
            String reason
    ) {

        /**
         * Key-value parameter.
         *
         * @param name parameter name
         * @param value parameter value
         */
        public record Parameter(String name, String value) { }
    }

    /**
     * Creates a RemediationRecommendation from a plan with approval metadata.
     *
     * @param planId source plan ID
     * @param agentId source agent
     * @param alertId triggering alert
     * @param serviceId affected service
     * @param actions converted actions
     * @param approvalTier tier of approval
     * @param riskLevel risk level
     * @param confidenceScore confidence score
     * @param approvedBy approver identifier
     * @return new RemediationRecommendation
     */
    public static RemediationRecommendation create(
            UUID planId,
            String agentId,
            String alertId,
            String serviceId,
            List<Action> actions,
            ApprovalTier approvalTier,
            RiskLevel riskLevel,
            double confidenceScore,
            String approvedBy) {
        return new RemediationRecommendation(
                planId,
                agentId,
                alertId,
                serviceId,
                actions,
                approvalTier,
                riskLevel,
                confidenceScore,
                Instant.now(),
                approvedBy,
                null
        );
    }

    /**
     * Returns a new instance with publishedAt set.
     */
    public RemediationRecommendation withPublishedAt() {
        return new RemediationRecommendation(
                planId, agentId, alertId, serviceId, actions,
                approvalTier, riskLevel, confidenceScore,
                approvedAt, approvedBy, Instant.now()
        );
    }

    /**
     * Returns true if this recommendation has been published.
     */
    public boolean isPublished() {
        return publishedAt != null;
    }
}