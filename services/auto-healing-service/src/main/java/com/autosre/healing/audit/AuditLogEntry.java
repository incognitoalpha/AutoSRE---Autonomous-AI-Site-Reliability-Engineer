package com.autosre.healing.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Persists every healing action attempt for audit and compliance.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

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

    public static AuditLogEntry create(String planId, String actionType, String target,
                                       String executor, String outcome, long durationMs,
                                       String errorMessage) {
        AuditLogEntry entry = new AuditLogEntry();
        entry.planId = planId;
        entry.actionType = actionType;
        entry.target = target;
        entry.executor = executor;
        entry.outcome = outcome;
        entry.durationMs = durationMs;
        entry.errorMessage = errorMessage;
        entry.executedAt = Instant.now();
        return entry;
    }

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