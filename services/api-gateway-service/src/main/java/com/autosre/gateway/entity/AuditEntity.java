package com.autosre.gateway.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for audit log entries from auto-healing-service.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String planId;

    @Column(nullable = false)
    private String actionType;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private String executor;

    @Column(nullable = false)
    private String outcome;

    @Column(nullable = false)
    private long durationMs;

    private String errorMessage;

    @Column(nullable = false)
    private Instant executedAt;

    public Long getId() { return id; }
    public String getPlanId() { return planId; }
    public String getActionType() { return actionType; }
    public String getTarget() { return target; }
    public String getExecutor() { return executor; }
    public String getOutcome() { return outcome; }
    public long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getExecutedAt() { return executedAt; }
}