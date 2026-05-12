package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;

/**
 * Interface for approval gates that route remediation plans through
 * different approval tiers based on risk level.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
public sealed interface ApprovalGate
        permits AutoApprovalGate, AsyncApprovalGate, SyncApprovalGate {

    /**
     * Determines whether this gate handles the given risk level.
     *
     * @param riskLevel the risk level to check
     * @return true if this gate supports the given risk level
     */
    boolean supports(RemediationRecommendation.RiskLevel riskLevel);

    /**
     * Processes the remediation plan through this gate.
     * Returns an approval decision indicating whether the plan is approved,
     * pending, or rejected.
     *
     * @param plan the remediation plan to process
     * @return the approval decision
     */
    ApprovalDecision process(RemediationPlanWrapper plan);

    /**
     * Represents the outcome of an approval gate decision.
     */
    enum ApprovalDecision {
        /** Plan is approved and ready for execution. */
        APPROVED,
        /** Plan is pending approval (async wait or human approval required). */
        PENDING,
        /** Plan is rejected and will not be executed. */
        REJECTED
    }

    /**
     * Wrapper class for remediation plan data passed to gates.
     */
    record RemediationPlanWrapper(
            java.util.UUID planId,
            String agentId,
            String alertId,
            String serviceId,
            String actionsJson,
            RemediationRecommendation.RiskLevel riskLevel,
            double confidenceScore,
            RemediationRecommendation.ApprovalTier tier
    ) {
    }
}