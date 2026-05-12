package com.autosre.recommendation.gate;

import com.autosre.recommendation.model.PendingApproval;
import com.autosre.recommendation.model.RemediationRecommendation;
import com.autosre.recommendation.producer.RemediationProducer;
import com.autosre.recommendation.repository.PendingApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Approval gate for high-risk remediation plans.
 * Stores plan in PostgreSQL pending_approvals table.
 * Blocks until explicit REST approval or timeout.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public final class SyncApprovalGate implements ApprovalGate {

    private static final Logger LOG = LoggerFactory.getLogger(SyncApprovalGate.class);

    private final PendingApprovalRepository repository;
    private final RemediationProducer producer;

    public SyncApprovalGate(
            PendingApprovalRepository repository,
            RemediationProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @Override
    public boolean supports(RemediationRecommendation.RiskLevel riskLevel) {
        return riskLevel == RemediationRecommendation.RiskLevel.HIGH;
    }

    @Override
    public ApprovalDecision process(RemediationPlanWrapper plan) {
        LOG.info("SyncApprovalGate processing plan: planId={}, confidence={}",
                plan.planId(), plan.confidenceScore());

        try {
            PendingApproval approval = PendingApproval.create(
                    plan.planId(),
                    plan.agentId(),
                    plan.alertId(),
                    plan.serviceId(),
                    plan.actionsJson(),
                    plan.riskLevel(),
                    plan.confidenceScore(),
                    RemediationRecommendation.ApprovalTier.SYNC
            );

            repository.save(approval);
            LOG.info("Plan {} stored in pending_approvals table, awaiting human approval",
                    plan.planId());

            return ApprovalDecision.PENDING;

        } catch (Exception e) {
            LOG.error("Failed to store plan {} for sync approval: {}",
                    plan.planId(), e.getMessage(), e);
            return ApprovalDecision.REJECTED;
        }
    }

    /**
     * Approves a pending plan by ID.
     */
    public boolean approve(UUID planId, String approvedBy) {
        Optional<PendingApproval> approvalOpt = repository.findByPlanId(planId);

        if (approvalOpt.isEmpty()) {
            LOG.warn("Pending approval not found for planId: {}", planId);
            return false;
        }

        PendingApproval approval = approvalOpt.get();

        if (approval.isExpired()) {
            LOG.warn("Pending approval for planId {} has expired", planId);
            return false;
        }

        approval.approve(approvedBy);
        repository.save(approval);

        try {
            producer.publishApproved(wrap(approval));
            LOG.info("Plan {} approved by {} and published", planId, approvedBy);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to publish approved plan {}: {}", planId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Rejects a pending plan by ID.
     */
    public boolean reject(UUID planId, String rejectedBy, String reason) {
        Optional<PendingApproval> approvalOpt = repository.findByPlanId(planId);

        if (approvalOpt.isEmpty()) {
            LOG.warn("Pending approval not found for planId: {}", planId);
            return false;
        }

        PendingApproval approval = approvalOpt.get();
        approval.reject(rejectedBy, reason);
        repository.save(approval);

        LOG.info("Plan {} rejected by {}: {}", planId, rejectedBy, reason);
        return true;
    }

    private RemediationPlanWrapper wrap(PendingApproval approval) {
        return new RemediationPlanWrapper(
                approval.getPlanId(),
                approval.getAgentId(),
                approval.getAlertId(),
                approval.getServiceId(),
                approval.getActionsJson(),
                approval.getRiskLevel(),
                approval.getConfidenceScore(),
                RemediationRecommendation.ApprovalTier.SYNC
        );
    }
}