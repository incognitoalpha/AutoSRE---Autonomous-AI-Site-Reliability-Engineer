package com.autosre.healing.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for persisting and querying audit LOG entries.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Service
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);
    private static final String EXECUTOR_NAME = "auto-healing-service";

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists an audit LOG entry for a healing action.
     *
     * @param planId       the remediation plan ID
     * @param actionType   the type of action (SCALE_DEPLOYMENT, RESTART_POD, etc.)
     * @param target       the resource target
     * @param outcome      SUCCESS or FAILURE
     * @param durationMs   execution duration in milliseconds
     * @param errorMessage optional error message if outcome is FAILURE
     */
    @Transactional
    public void logAction(String planId, String actionType, String target,
                          String outcome, long durationMs, String errorMessage) {
        AuditLogEntry entry = AuditLogEntry.create(
                planId, actionType, target, EXECUTOR_NAME, outcome, durationMs, errorMessage);
        repository.save(entry);
        LOG.info("Audit LOG persisted: planId={}, action={}, target={}, outcome={}",
                planId, actionType, target, outcome);
    }

    /**
     * Retrieves audit LOG entries for a specific plan.
     *
     * @param planId the remediation plan ID
     * @return list of audit entries for the plan
     */
    public List<AuditLogEntry> findByPlanId(String planId) {
        return repository.findByPlanId(planId);
    }

    /**
     * Retrieves audit entries within a time range.
     *
     * @param startTime start of the time range
     * @param endTime   end of the time range
     * @return list of audit entries within the range
     */
    public List<AuditLogEntry> findByTimeRange(long startTime, long endTime) {
        return repository.findByExecutedAtBetween(startTime, endTime);
    }
}