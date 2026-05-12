package com.autosre.common.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immutable record representing a detected anomaly alert.
 * Published to Kafka for consumption by downstream services.
 *
 * <p>Bounded context: shared module — cross-service contract</p>
 *
 * @param alertId unique identifier for this alert
 * @param serviceId the service experiencing the anomaly
 * @param severity the severity level of the anomaly
 * @param metricName the metric that triggered the alert
 * @param metricValue the actual metric value that was detected as anomalous
 * @param baselineStats statistical baseline used for detection
 * @param detectorType the type of detector that triggered (e.g., "ZSCORE", "MAD", "ONNX")
 * @param confidenceScore confidence score from 0.0 to 1.0
 * @param detectedAt when the anomaly was detected
 * @param traceId OpenTelemetry trace identifier for correlation
 * @param affectedComponents list of affected service components
 */
public record AnomalyAlert(
    UUID alertId,
    String serviceId,
    Severity severity,
    String metricName,
    double metricValue,
    BaselineStats baselineStats,
    String detectorType,
    double confidenceScore,
    Instant detectedAt,
    String traceId,
    List<String> affectedComponents
) {
    /**
     * Statistical baseline used for anomaly detection.
     *
     * @param mean the arithmetic mean of the baseline
     * @param stdDev the standard deviation (for Z-score)
     * @param median the median of the baseline (for MAD)
     * @param mad the median absolute deviation (for MAD)
     */
    public record BaselineStats(
        double mean,
        double stdDev,
        double median,
        double mad
    ) {
    }

    public AnomalyAlert {
        if (alertId == null) {
            alertId = UUID.randomUUID();
        }
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be null or blank");
        }
        if (severity == null) {
            severity = Severity.HIGH;
        }
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("metricName must not be null or blank");
        }
        if (baselineStats == null) {
            baselineStats = new BaselineStats(0, 0, 0, 0);
        }
        if (detectorType == null || detectorType.isBlank()) {
            detectorType = "UNKNOWN";
        }
        if (confidenceScore < 0 || confidenceScore > 1) {
            confidenceScore = 0.5;
        }
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
        if (affectedComponents == null) {
            affectedComponents = List.of();
        }
    }

    /**
     * Factory method to create a critical severity alert.
     *
     * @param serviceId the affected service
     * @param metricName the triggering metric
     * @param metricValue the anomalous value
     * @param baselineStats the statistical baseline
     * @param detectorType the detector type
     * @return a new AnomalyAlert with CRITICAL severity
     */
    public static AnomalyAlert critical(String serviceId, String metricName,
            double metricValue, BaselineStats baselineStats, String detectorType) {
        return new AnomalyAlert(UUID.randomUUID(), serviceId, Severity.CRITICAL,
            metricName, metricValue, baselineStats, detectorType, 0.95,
            Instant.now(), null, List.of(serviceId));
    }

    /**
     * Factory method to create a high severity alert.
     *
     * @param serviceId the affected service
     * @param metricName the triggering metric
     * @param metricValue the anomalous value
     * @param baselineStats the statistical baseline
     * @param detectorType the detector type
     * @return a new AnomalyAlert with HIGH severity
     */
    public static AnomalyAlert high(String serviceId, String metricName,
            double metricValue, BaselineStats baselineStats, String detectorType) {
        return new AnomalyAlert(UUID.randomUUID(), serviceId, Severity.HIGH,
            metricName, metricValue, baselineStats, detectorType, 0.8,
            Instant.now(), null, List.of(serviceId));
    }

    /**
     * Returns a summary string for logging.
     *
     * @return formatted alert summary
     */
    public String toAlertSummary() {
        return String.format("Alert[alertId=%s, service=%s, severity=%s, metric=%s, value=%.2f, detector=%s]",
            alertId, serviceId, severity, metricName, metricValue, detectorType);
    }
}
