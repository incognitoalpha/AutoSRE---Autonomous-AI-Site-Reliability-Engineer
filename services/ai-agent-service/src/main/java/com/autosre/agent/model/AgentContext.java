package com.autosre.agent.model;

import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.List;

/**
 * Carries the full execution context for a specialist agent during RCA and remediation.
 * This record threads together the triggering alert, retrieved knowledge, and the
 * agent's ongoing reasoning trace throughout the multi-agent pipeline.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @param alert the triggering anomaly alert
 * @param retrievedRunbooks top-3 relevant runbook chunks from pgvector similarity search
 * @param historicalIncidents recent incidents for the same service to inform pattern matching
 * @param reasoningTrace current agent reasoning steps for auditability
 */
public record AgentContext(
        String alertId,
        String serviceId,
        Severity severity,
        Instant detectedAt,
        List<String> retrievedRunbooks,
        List<HistoricalIncident> historicalIncidents,
        String reasoningTrace
) {

    /**
     * Lightweight reference to historical incidents used for context enrichment.
     * Embeddings are stored separately in pgvector; this carries metadata only.
     *
     * @param incidentId UUID of the historical incident
     * @param serviceId the affected service
     * @param rootCauseSummary brief description of the root cause
     * @param resolvedAt when the incident was resolved
     */
    public record HistoricalIncident(
            String incidentId,
            String serviceId,
            String rootCauseSummary,
            Instant resolvedAt
    ) { }
}