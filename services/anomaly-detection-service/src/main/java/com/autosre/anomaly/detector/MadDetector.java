package com.autosre.anomaly.detector;

import java.util.ArrayList;
import java.util.Collections;
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
 * Median Absolute Deviation (MAD) anomaly detector.
 * More robust than Z-score for datasets with outliers, as it uses
 * the median instead of mean for baseline calculation.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Median_absolute_deviation">MAD Algorithm</a>
 */
@Component
@ConfigurationProperties(prefix = "autosre.detection.mad")
public class MadDetector implements AnomalyDetector {

    private static final Logger LOG = LoggerFactory.getLogger(MadDetector.class);
    private static final String DETECTOR_TYPE = "MAD";
    private static final double DEFAULT_SENSITIVITY = 1.4826;

    private double threshold = 3.5;
    private double sensitivity = DEFAULT_SENSITIVITY;
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
     * Sets the MAD threshold (modified Z-score threshold).
     *
     * @param threshold the threshold value (typically 3.0 to 4.0)
     */
    public void setThreshold(double threshold) {
        if (threshold <= 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }
        this.threshold = threshold;
    }

    /**
     * Sets the sensitivity constant (default 1.4826 for normal distribution).
     *
     * @param sensitivity the sensitivity constant
     */
    public void setSensitivity(double sensitivity) {
        if (sensitivity <= 0) {
            throw new IllegalArgumentException("Sensitivity must be positive");
        }
        this.sensitivity = sensitivity;
    }

    /**
     * Sets the minimum baseline size required for detection.
     *
     * @param minBaselineSize minimum number of samples
     */
    public void setMinBaselineSize(int minBaselineSize) {
        if (minBaselineSize < 5) {
            throw new IllegalArgumentException("Minimum baseline size must be at least 5 for MAD");
        }
        this.minBaselineSize = minBaselineSize;
    }

    /**
     * {@inheritDoc}
     *
     * Uses Modified Z-Score: modified_z = (0.6745 * (value - median)) / MAD
     * Values with |modified_z| > threshold are considered anomalous.
     * When baseline has zero MAD (constant signal), falls back to
     * range-based detection: any deviation from the constant value is flagged.
     */
    @Override
    public Optional<AnomalyAlert> detect(TelemetryEvent event, List<Double> baseline) {
        if (event == null) {
            throw new IllegalArgumentException("TelemetryEvent must not be null");
        }
        if (baseline == null || baseline.size() < minBaselineSize) {
            LOG.warn("Insufficient baseline samples for MAD: {} < {} for service {}",
                baseline != null ? baseline.size() : 0, minBaselineSize, event.serviceId());
            return Optional.empty();
        }

        double value = event.value();
        List<Double> sortedBaseline = new ArrayList<>(baseline);
        Collections.sort(sortedBaseline);

        double median = calculateMedian(sortedBaseline);
        double mad = calculateMad(sortedBaseline, median);

        // Constant baseline fallback: any deviation from constant value is an anomaly
        if (mad == 0) {
            LOG.debug("Constant baseline detected for service {}, using range-based detection", event.serviceId());
            if (baseline.isEmpty() || value != baseline.get(0)) {
                double constantValue = baseline.get(0);
                double deviation = Math.abs(value - constantValue);
                double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(median);
                BaselineStats stats = new BaselineStats(mean, 0, median, 0);
                Severity severity = determineSeverityFromDeviation(deviation, constantValue);

                AnomalyAlert alert = new AnomalyAlert(
                    java.util.UUID.randomUUID(),
                    event.serviceId(),
                    severity,
                    event.metricName(),
                    value,
                    stats,
                    DETECTOR_TYPE,
                    0.95,
                    java.time.Instant.now(),
                    event.traceId(),
                    List.of(event.serviceId())
                );

                LOG.info("Anomaly detected via MAD (constant baseline): service={}, "
                        + "metric={}, value={}, constant={}, deviation={}",
                        event.serviceId(), event.metricName(), value, constantValue, deviation);
                return Optional.of(alert);
            }
            return Optional.empty();
        }

        double modifiedZ = (0.6745 * (value - median)) / mad;
        double absModifiedZ = Math.abs(modifiedZ);

        LOG.debug("MAD analysis: value={}, median={}, mad={}, modifiedZ={}, threshold={}",
            value, median, mad, modifiedZ, threshold);

        if (absModifiedZ > threshold) {
            double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(median);
            double stdDev = calculateStdDev(baseline, mean);
            BaselineStats stats = new BaselineStats(mean, stdDev, median, mad);

            double confidence = calculateConfidence(absModifiedZ);
            Severity severity = determineSeverity(absModifiedZ);

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

            LOG.info("Anomaly detected via MAD: service={}, metric={}, value={}, modifiedZ={}",
                event.serviceId(), event.metricName(), value, modifiedZ);
            return Optional.of(alert);
        }

        return Optional.empty();
    }

    private double calculateMedian(List<Double> sortedValues) {
        int size = sortedValues.size();
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }

    private double calculateMad(List<Double> sortedValues, double median) {
        List<Double> deviations = new ArrayList<>();
        for (Double value : sortedValues) {
            deviations.add(Math.abs(value - median));
        }
        return calculateMedian(deviations) * sensitivity;
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }

    private double calculateConfidence(double absModifiedZ) {
        double maxZ = threshold * 3;
        double normalized = Math.min(absModifiedZ, maxZ) / maxZ;
        return Math.min(0.99, 0.5 + (normalized * 0.49));
    }

    private Severity determineSeverity(double absModifiedZ) {
        if (absModifiedZ > threshold * 2.5) {
            return Severity.CRITICAL;
        } else if (absModifiedZ > threshold * 1.5) {
            return Severity.HIGH;
        } else {
            return Severity.MEDIUM;
        }
    }

    private Severity determineSeverityFromDeviation(double deviation, double baselineValue) {
        if (baselineValue == 0) {
            return deviation > 0 ? Severity.CRITICAL : Severity.MEDIUM;
        }
        double relativeDeviation = deviation / Math.abs(baselineValue);
        if (relativeDeviation > 1.0) {
            return Severity.CRITICAL;
        } else if (relativeDeviation > 0.5) {
            return Severity.HIGH;
        } else {
            return Severity.MEDIUM;
        }
    }
}