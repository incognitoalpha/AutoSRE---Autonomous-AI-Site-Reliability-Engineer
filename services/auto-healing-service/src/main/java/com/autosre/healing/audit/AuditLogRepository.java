package com.autosre.healing.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for AuditLogEntry persistence.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {

    List<AuditLogEntry> findByPlanId(String planId);

    List<AuditLogEntry> findByExecutedAtBetween(long startTime, long endTime);
}