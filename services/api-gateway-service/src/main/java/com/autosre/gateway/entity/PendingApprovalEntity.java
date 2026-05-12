package com.autosre.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for pending approval records from recommendation-service.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Entity
@Table(name = "pending_approvals")
public class PendingApprovalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String planId;

    @Column(nullable = false)
    private String agentId;

    @Column(nullable = false)
    private String actionsJson;

    @Column(nullable = false)
    private String approvalTier;

    @Column(nullable = false)
    private double confidenceScore;

    @Column(nullable = false)
    private long createdAt;

    private long approvedAt;

    private long rejectedAt;

    private String decidedBy;

    private String status;

    public static PendingApprovalEntity create(String planId, String agentId, String actionsJson,
                                                String approvalTier, double confidenceScore) {
        PendingApprovalEntity entity = new PendingApprovalEntity();
        entity.planId = planId;
        entity.agentId = agentId;
        entity.actionsJson = actionsJson;
        entity.approvalTier = approvalTier;
        entity.confidenceScore = confidenceScore;
        entity.createdAt = Instant.now().toEpochMilli();
        entity.status = "PENDING";
        return entity;
    }

    public Long getId() { return id; }
    public String getPlanId() { return planId; }
    public String getAgentId() { return agentId; }
    public String getActionsJson() { return actionsJson; }
    public String getApprovalTier() { return approvalTier; }
    public double getConfidenceScore() { return confidenceScore; }
    public long getCreatedAt() { return createdAt; }
    public long getApprovedAt() { return approvedAt; }
    public long getRejectedAt() { return rejectedAt; }
    public String getDecidedBy() { return decidedBy; }
    public String getStatus() { return status; }

    public void approve(String decidedBy) {
        this.status = "APPROVED";
        this.approvedAt = Instant.now().toEpochMilli();
        this.decidedBy = decidedBy;
    }

    public void reject(String decidedBy) {
        this.status = "REJECTED";
        this.rejectedAt = Instant.now().toEpochMilli();
        this.decidedBy = decidedBy;
    }
}