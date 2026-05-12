package com.autosre.gateway.entity;

import com.autosre.common.model.Severity;
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
 * JPA entity representing a resolved incident stored in PostgreSQL.
 *
 * <p>Bounded context: {@code api-gateway-service}</p>
 */
@Entity
@Table(name = "incidents")
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String serviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String rootCause;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant detectedAt;

    private Instant resolvedAt;

    public static IncidentEntity create(String serviceId, Severity severity, String rootCause) {
        IncidentEntity entity = new IncidentEntity();
        entity.serviceId = serviceId;
        entity.severity = severity;
        entity.rootCause = rootCause;
        entity.status = "OPEN";
        entity.detectedAt = Instant.now();
        return entity;
    }

    public UUID getId() { return id; }
    public String getServiceId() { return serviceId; }
    public Severity getSeverity() { return severity; }
    public String getRootCause() { return rootCause; }
    public String getStatus() { return status; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void markResolved() {
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }

    public void markInvestigating() {
        this.status = "INVESTIGATING";
    }
}