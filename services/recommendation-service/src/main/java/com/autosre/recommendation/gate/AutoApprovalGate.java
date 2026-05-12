package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Approval gate for low-risk remediation plans.
 * Supports plans with LOW risk level and high confidence.
 * Publishes directly to the remediation Kafka topic without delay.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public final class AutoApprovalGate implements ApprovalGate {

    private static final Logger LOG = LoggerFactory.getLogger(AutoApprovalGate.class);
    private static final double MIN_CONFIDENCE = 0.90;

    private final RemediationProducer producer;

    public AutoApprovalGate(RemediationProducer producer) {
        this.producer = producer;
    }

    @Override
    public boolean supports(RemediationRecommendation.RiskLevel riskLevel) {
        return riskLevel == RemediationRecommendation.RiskLevel.LOW;
    }

    @Override
    public ApprovalDecision process(RemediationPlanWrapper plan) {
        LOG.info("AutoApprovalGate processing plan: planId={}, confidence={}",
                plan.planId(), plan.confidenceScore());

        if (plan.confidenceScore() < MIN_CONFIDENCE) {
            LOG.warn("Plan {} confidence {} below minimum {}, routing to higher approval",
                    plan.planId(), plan.confidenceScore(), MIN_CONFIDENCE);
            return ApprovalDecision.PENDING;
        }

        try {
            producer.publishApproved(plan);
            LOG.info("Plan {} auto-approved and published", plan.planId());
            return ApprovalDecision.APPROVED;
        } catch (Exception e) {
            LOG.error("Failed to publish plan {}: {}", plan.planId(), e.getMessage(), e);
            return ApprovalDecision.REJECTED;
        }
    }
}