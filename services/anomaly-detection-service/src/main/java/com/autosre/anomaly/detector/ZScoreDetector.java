package com.autosre.anomaly.detector;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.autosre.common.model.AnomalyAlert.BaselineStats;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;
import com.autosre.common.model.Severity;

/**
 * Z-Score anomaly detector using standard deviation threshold.
 * Detects values that deviate more than N standard deviations from the mean.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Standard_score">Z-Score Algorithm</a>
 */
@Component
@ConfigurationProperties(prefix = "autosre.detection.zscore")
public class ZScoreDetector implements AnomalyDetector {

    private static final Logger LOG = LoggerFactory.getLogger(ZScoreDetector.class);
    private static final String DETECTOR_TYPE = "ZSCORE";

    private double threshold = 3.0;
    private int minBaselineSize = 30;

    @Override
    public String getDetectorType() {
        return DETECTOR_TYPE;
    }

    @Override
    public int getMinBaselineSize() {
        return minBaselineSize;
    }

    @Override
    public double getThreshold() {
        return threshold;
    }

    /**
     * Sets the Z-score threshold (number of standard deviations).
     *
     * @param threshold the threshold value (typically 2.5 to 4.0)
     */
    public void setThreshold(double threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }
        this.threshold = threshold;
    }

    /**
     * Sets the minimum baseline size required for detection.
     *
     * @param minBaselineSize minimum number of samples
     */
    public void setMinBaselineSize(int minBaselineSize) {
        if (minBaselineSize < 2) {
            throw new IllegalArgumentException("Minimum baseline size must be at least 2");
        }
        this.minBaselineSize = minBaselineSize;
    }

    /**
     * {@inheritDoc}
     *
     * Uses Z-score formula: z = (value - mean) / stdDev
     * If |z| > threshold, the value is considered anomalous.
     * When baseline has zero variance (constant signal), falls back to
     * range-based detection: any deviation from the constant value is flagged.
     */
    @Override
    public Optional<AnomalyAlert> detect(TelemetryEvent event, List<Double> baseline) {
        if (event == null) {
            throw new IllegalArgumentException("TelemetryEvent must not be null");
        }
        if (baseline == null || baseline.size() < minBaselineSize) {
            LOG.warn("Insufficient baseline samples: {} < {} for service {}",
                baseline != null ? baseline.size() : 0, minBaselineSize, event.serviceId());
            return Optional.empty();
        }

        double value = event.value();
        double mean = calculateMean(baseline);
        double stdDev = calculateStdDev(baseline, mean);

        // Constant baseline fallback: any deviation from constant value is an anomaly
        if (stdDev == 0) {
            LOG.debug("Constant baseline detected for service {}, using range-based detection", event.serviceId());
            if (baseline.isEmpty() || value != baseline.get(0)) {
                // Baseline is constant [c, c, c...] but value != c → anomaly
                double constantValue = baseline.get(0);
                double deviation = Math.abs(value - constantValue);
                BaselineStats stats = new BaselineStats(mean, 0, 0, 0);
                Severity severity = determineSeverityFromDeviation(deviation, constantValue);

                AnomalyAlert alert = new AnomalyAlert(
                    java.util.UUID.randomUUID(),
                    event.serviceId(),
                    severity,
                    event.metricName(),
                    value,
                    stats,
                    DETECTOR_TYPE,
                    0.95,  // high confidence — any deviation from constant is significant
                    java.time.Instant.now(),
                    event.traceId(),
                    List.of(event.serviceId())
                );

                LOG.info("Anomaly detected via Z-Score (constant baseline): service={}, "
                        + "metric={}, value={}, constant={}, deviation={}",
                        event.serviceId(), event.metricName(), value, constantValue, deviation);
                return Optional.of(alert);
            }
            return Optional.empty();
        }

        double zScore = Math.abs((value - mean) / stdDev);

        LOG.debug("Z-Score analysis: value={}, mean={}, stdDev={}, zScore={}, threshold={}",
            value, mean, stdDev, zScore, threshold);

        if (zScore > threshold) {
            BaselineStats stats = new BaselineStats(mean, stdDev, 0, 0);
            double confidence = calculateConfidence(zScore, stdDev);
            Severity severity = determineSeverity(zScore);

            AnomalyAlert alert = new AnomalyAlert(
                java.util.UUID.randomUUID(),
                event.serviceId(),
                severity,
                event.metricName(),
                value,
                stats,
                DETECTOR_TYPE,
                confidence,
                java.time.Instant.now(),
                event.traceId(),
                List.of(event.serviceId())
            );

            LOG.info("Anomaly detected via Z-Score: service={}, metric={}, value={}, z={}",
                event.serviceId(), event.metricName(), value, zScore);
            return Optional.of(alert);
        }

        return Optional.empty();
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }

    private double calculateConfidence(double zScore, double stdDev) {
        double maxZScore = threshold * 3;
        double normalized = Math.min(zScore, maxZScore) / maxZScore;
        return Math.min(0.99, 0.5 + (normalized * 0.49));
    }

    private com.autosre.common.model.Severity determineSeverity(double zScore) {
        if (zScore > threshold * 2) {
            return com.autosre.common.model.Severity.CRITICAL;
        } else if (zScore > threshold * 1.5) {
            return com.autosre.common.model.Severity.HIGH;
        } else {
            return com.autosre.common.model.Severity.MEDIUM;
        }
    }

    private com.autosre.common.model.Severity determineSeverityFromDeviation(double deviation, double baselineValue) {
        if (baselineValue == 0) {
            return deviation > 0 ? com.autosre.common.model.Severity.CRITICAL : com.autosre.common.model.Severity.MEDIUM;
        }
        double relativeDeviation = deviation / Math.abs(baselineValue);
        if (relativeDeviation > 1.0) {
            return com.autosre.common.model.Severity.CRITICAL;
        } else if (relativeDeviation > 0.5) {
            return com.autosre.common.model.Severity.HIGH;
        } else {
            return com.autosre.common.model.Severity.MEDIUM;
        }
    }
}