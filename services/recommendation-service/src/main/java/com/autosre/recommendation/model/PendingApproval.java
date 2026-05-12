package com.autosre.recommendation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a pending approval that requires human intervention.
 * Stored in PostgreSQL until explicitly approved or rejected via REST API.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Entity
@Table(name = "pending_approvals")
public class PendingApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID planId;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String alertId;

    @Column(nullable = false)
    private String serviceId;

    @Column(columnDefinition = "TEXT")
    private String actionsJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationRecommendation.RiskLevel riskLevel;

    @Column(nullable = false)
    private double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemediationRecommendation.ApprovalTier tier;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant approvedAt;

    @Column
    private Instant rejectedAt;

    @Column
    private String approvedBy;

    @Column
    private String rejectionReason;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean notified;

    protected PendingApproval() {
    }

    /**
     * Factory method to create a new pending approval.
     */
    public static PendingApproval create(
            UUID planId,
            String agentId,
            String alertId,
            String serviceId,
            String actionsJson,
            RemediationRecommendation.RiskLevel riskLevel,
            double confidenceScore,
            RemediationRecommendation.ApprovalTier tier) {
        PendingApproval approval = new PendingApproval();
        approval.planId = planId;
        approval.agentId = agentId;
        approval.alertId = alertId;
        approval.serviceId = serviceId;
        approval.actionsJson = actionsJson;
        approval.riskLevel = riskLevel;
        approval.confidenceScore = confidenceScore;
        approval.tier = tier;
        approval.createdAt = Instant.now();
        approval.expiresAt = Instant.now().plusSeconds(3600);
        approval.notified = false;
        return approval;
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getPlanId() { return planId; }
    public String getAgentId() { return agentId; }
    public String getAlertId() { return alertId; }
    public String getServiceId() { return serviceId; }
    public String getActionsJson() { return actionsJson; }
    public RemediationRecommendation.RiskLevel getRiskLevel() { return riskLevel; }
    public double getConfidenceScore() { return confidenceScore; }
    public RemediationRecommendation.ApprovalTier getTier() { return tier; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getRejectedAt() { return rejectedAt; }
    public String getApprovedBy() { return approvedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isNotified() { return notified; }

    /**
     * Marks this approval as approved by the given approver.
     */
    public void approve(String approvedBy) {
        this.approvedAt = Instant.now();
        this.approvedBy = approvedBy;
    }

    /**
     * Marks this approval as rejected.
     */
    public void reject(String rejectedBy, String reason) {
        this.rejectedAt = Instant.now();
        this.approvedBy = rejectedBy;
        this.rejectionReason = reason;
    }

    /**
     * Marks this approval as notified.
     */
    public void markNotified() {
        this.notified = true;
    }

    /**
     * Returns true if this approval has been processed (approved or rejected).
     */
    public boolean isProcessed() {
        return approvedAt != null || rejectedAt != null;
    }

    /**
     * Returns true if this approval has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}