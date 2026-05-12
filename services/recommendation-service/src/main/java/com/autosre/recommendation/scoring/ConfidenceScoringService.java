package com.autosre.recommendation.scoring;


import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that applies business rules to compute confidence scores for remediation plans.
 * Adjusts base confidence based on historical resolution rates, runbook relevance,
 * and other contextual factors.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Service
public class ConfidenceScoringService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfidenceScoringService.class);

    private static final double BASE_CONFIDENCE = 0.75;
    private static final double HIGH_RUNBOOK_BOOST = 0.15;
    private static final double LOW_RESOLUTION_PENALTY = 0.20;
    private static final double SAME_ROOT_CAUSE_PENALTY = 0.10;
    private static final double CRITICAL_SEVERITY_BOOST = 0.05;

    private final Map<String, Double> historicalResolutionRates;

    public ConfidenceScoringService() {
        this.historicalResolutionRates = new HashMap<>();
        initializeDefaults();
    }

    /**
     * Initializes default historical resolution rates for common root causes.
     */
    private void initializeDefaults() {
        historicalResolutionRates.put("memory_exhaustion", 0.85);
        historicalResolutionRates.put("cpu_saturation", 0.90);
        historicalResolutionRates.put("OOM", 0.80);
        historicalResolutionRates.put("crashloop", 0.75);
        historicalResolutionRates.put("deployment_regression", 0.95);
        historicalResolutionRates.put("network_issue", 0.70);
        historicalResolutionRates.put("disk_full", 0.88);
    }

    /**
     * Computes the adjusted confidence score for a remediation plan.
     *
     * @param baseConfidence the base confidence from the specialist agent
     * @param rootCause the identified root cause
     * @param hasRunbookMatch true if a relevant runbook was found
     * @param isCritical true if the incident is critical severity
     * @param similarIncidentCount number of similar historical incidents
     * @return adjusted confidence score between 0.0 and 1.0
     */
    public double computeScore(
            double baseConfidence,
            String rootCause,
            boolean hasRunbookMatch,
            boolean isCritical,
            int similarIncidentCount) {

        double adjustedScore = baseConfidence;

        if (hasRunbookMatch) {
            adjustedScore += HIGH_RUNBOOK_BOOST;
            LOG.debug("Boosted score by {} for runbook match", HIGH_RUNBOOK_BOOST);
        }

        Double resolutionRate = historicalResolutionRates.get(normalizeRootCause(rootCause));
        if (resolutionRate != null && resolutionRate < 0.8) {
            adjustedScore -= LOW_RESOLUTION_PENALTY;
            LOG.debug("Penalized score by {} for low resolution rate: {}",
                    LOW_RESOLUTION_PENALTY, resolutionRate);
        }

        if (similarIncidentCount > 0) {
            adjustedScore += SAME_ROOT_CAUSE_PENALTY;
            LOG.debug("Boosted score by {} for {} similar incidents",
                    SAME_ROOT_CAUSE_PENALTY, similarIncidentCount);
        }

        if (isCritical) {
            adjustedScore += CRITICAL_SEVERITY_BOOST;
            LOG.debug("Boosted score by {} for critical severity", CRITICAL_SEVERITY_BOOST);
        }

        adjustedScore = Math.max(0.0, Math.min(1.0, adjustedScore));
        LOG.info("Computed confidence score: base={}, adjusted={}", baseConfidence, adjustedScore);

        return adjustedScore;
    }

    /**
     * Updates the historical resolution rate for a root cause.
     *
     * @param rootCause the root cause identifier
     * @param success true if the remediation was successful
     */
    public void recordResolution(String rootCause, boolean success) {
        String normalized = normalizeRootCause(rootCause);
        Double currentRate = historicalResolutionRates.get(normalized);

        if (currentRate == null) {
            currentRate = success ? 1.0 : 0.0;
        } else {
            currentRate = (currentRate * 0.8) + (success ? 0.2 : 0.0);
        }

        historicalResolutionRates.put(normalized, currentRate);
        LOG.info("Updated resolution rate for {}: {}", normalized, currentRate);
    }

    /**
     * Normalizes a root cause string for lookup.
     */
    private String normalizeRootCause(String rootCause) {
        if (rootCause == null) {
            return "unknown";
        }
        return rootCause.toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");
    }

    /**
     * Returns the current resolution rate for a root cause.
     */
    public double getResolutionRate(String rootCause) {
        Double rate = historicalResolutionRates.get(normalizeRootCause(rootCause));
        return rate != null ? rate : 0.5;
    }
}