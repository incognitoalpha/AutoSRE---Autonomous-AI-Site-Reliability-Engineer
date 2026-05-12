package com.autosre.recommendation.scoring;

import com.autosre.recommendation.model.RemediationRecommendation;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Classifies remediation plans into risk levels based on action type and blast radius.
 * Uses predefined rules to ensure consistent risk assessment.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Service
public class RiskLevelClassifier {

    private static final Logger LOG = LoggerFactory.getLogger(RiskLevelClassifier.class);

    private static final Set<String> HIGH_RISK_ACTIONS = Set.of(
            "ROLLBACK_DEPLOYMENT",
            "DRAIN_NODE",
            "QUARANTINE_POD"
    );

    private static final Set<String> MEDIUM_RISK_ACTIONS = Set.of(
            "SCALE_DEPLOYMENT",
            "SCALE_BROKERS",
            "ADJUST_RESOURCE_LIMITS"
    );

    private static final Set<String> LOW_RISK_ACTIONS = Set.of(
            "RESTART_POD",
            "NOTIFY_ONCALL"
    );

    /**
     * Classifies a remediation plan's risk level based on its actions.
     *
     * @param planId the plan identifier
     * @param actionTypes the set of action types in the plan
     * @param serviceCount number of affected services
     * @return the classified risk level
     */
    public RemediationRecommendation.RiskLevel classify(
            String planId,
            Set<String> actionTypes,
            int serviceCount) {

        if (actionTypes == null || actionTypes.isEmpty()) {
            LOG.warn("No action types provided for plan {}, defaulting to MEDIUM", planId);
            return RemediationRecommendation.RiskLevel.MEDIUM;
        }

        for (String action : actionTypes) {
            if (HIGH_RISK_ACTIONS.contains(action)) {
                LOG.info("Plan {} classified as HIGH risk due to action: {}", planId, action);
                return RemediationRecommendation.RiskLevel.HIGH;
            }
        }

        for (String action : actionTypes) {
            if (MEDIUM_RISK_ACTIONS.contains(action)) {
                LOG.info("Plan {} classified as MEDIUM risk due to action: {}", planId, action);
                return RemediationRecommendation.RiskLevel.MEDIUM;
            }
        }

        if (serviceCount > 3) {
            LOG.info("Plan {} classified as MEDIUM risk due to blast radius: {} services",
                    planId, serviceCount);
            return RemediationRecommendation.RiskLevel.MEDIUM;
        }

        LOG.info("Plan {} classified as LOW risk", planId);
        return RemediationRecommendation.RiskLevel.LOW;
    }

    /**
     * Classifies risk based on confidence score and action types.
     * Higher confidence with low-risk actions allows auto-approval.
     *
     * @param confidenceScore the confidence score (0.0 to 1.0)
     * @param actionTypes the action types in the plan
     * @return the classified risk level
     */
    public RemediationRecommendation.RiskLevel classifyByConfidence(
            double confidenceScore,
            Set<String> actionTypes) {

        if (confidenceScore >= 0.90 && isLowRiskOnly(actionTypes)) {
            return RemediationRecommendation.RiskLevel.LOW;
        }

        if (confidenceScore >= 0.75 && !hasHighRiskActions(actionTypes)) {
            return RemediationRecommendation.RiskLevel.MEDIUM;
        }

        return RemediationRecommendation.RiskLevel.HIGH;
    }

    private boolean isLowRiskOnly(Set<String> actionTypes) {
        return actionTypes.stream().allMatch(LOW_RISK_ACTIONS::contains);
    }

    private boolean hasHighRiskActions(Set<String> actionTypes) {
        return actionTypes.stream().anyMatch(HIGH_RISK_ACTIONS::contains);
    }
}