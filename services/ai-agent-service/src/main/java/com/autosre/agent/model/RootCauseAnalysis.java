package com.autosre.agent.model;

import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.List;

/**
 * Represents a structured root cause analysis produced by {@code RootCauseAgent}.
 * This record is the canonical output of the RCA pipeline and is stored
 * in PostgreSQL with an embedding for future similarity retrieval.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @param rootCause plain-language description of the root cause
 * @param affectedComponents list of service names or Kubernetes resource names impacted
 * @param confidenceScore a score between 0.0 and 1.0 indicating analysis confidence
 * @param recommendedActions list of suggested remediation actions to take
 * @param evidenceSummary key metrics, logs, and events that led to this conclusion
 * @param alertId the triggering anomaly alert ID for correlation
 * @param serviceId the affected service identifier
 * @param severity the severity of the triggering alert
 * @param analyzedAt when this analysis was completed
 */
public record RootCauseAnalysis(
        String rootCause,
        List<String> affectedComponents,
        double confidenceScore,
        List<String> recommendedActions,
        String evidenceSummary,
        String alertId,
        String serviceId,
        Severity severity,
        Instant analyzedAt
) {

    /**
     * Validates that the confidence score is within the valid range [0.0, 1.0].
     *
     * @throws IllegalArgumentException if confidenceScore is outside [0.0, 1.0]
     */
    public RootCauseAnalysis {
        if (confidenceScore < 0.0 || confidenceScore > 1.0) {
            throw new IllegalArgumentException(
                    "confidenceScore must be between 0.0 and 1.0, got: " + confidenceScore);
        }
        rootCause = (rootCause != null) ? rootCause.trim() : "";
        affectedComponents = (affectedComponents != null)
                ? List.copyOf(affectedComponents)
                : List.of();
        recommendedActions = (recommendedActions != null)
                ? List.copyOf(recommendedActions)
                : List.of();
        evidenceSummary = (evidenceSummary != null) ? evidenceSummary.trim() : "";
        analyzedAt = (analyzedAt != null) ? analyzedAt : Instant.now();
    }

    /**
     * Returns true if confidence is above the auto-execution threshold.
     *
     * @return true when confidenceScore >= 0.90
     */
    public boolean isHighConfidence() {
        return confidenceScore >= 0.90;
    }

    /**
     * Returns true when confidence is above the async approval threshold.
     *
     * @return true when confidenceScore >= 0.75
     */
    public boolean isActionable() {
        return confidenceScore >= 0.75;
    }
}