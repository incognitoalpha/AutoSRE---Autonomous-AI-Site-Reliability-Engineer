package com.autosre.anomaly.detector;

import java.util.List;
import java.util.Optional;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;

/**
 * Interface for anomaly detection algorithms.
 * Implementations analyze telemetry events against historical baselines
 * to detect statistical anomalies.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 *
 * @see ZScoreDetector
 * @see MadDetector
 * @see OnnxIsolationForestDetector
 */
public interface AnomalyDetector {

    /**
     * The name of this detector for logging and alert attribution.
     *
     * @return the detector type name
     */
    String getDetectorType();

    /**
     * Detects whether the given telemetry event is anomalous based on the baseline.
     *
     * @param event the telemetry event to evaluate; must not be null
     * @param baseline the historical baseline values; must contain at least the minimum samples
     * @return an Optional containing an AnomalyAlert if anomalous, empty otherwise
     * @throws IllegalArgumentException if event is null or baseline has insufficient samples
     */
    Optional<AnomalyAlert> detect(TelemetryEvent event, List<Double> baseline);

    /**
     * Returns the minimum number of baseline samples required for accurate detection.
     *
     * @return minimum baseline size
     */
    int getMinBaselineSize();

    /**
     * Returns the current threshold value for this detector.
     *
     * @return the threshold value
     */
    double getThreshold();

    /**
     * Returns true if this detector supports the given metric name.
     * Some detectors are specialized for specific metric types.
     *
     * @param metricName the metric name to check
     * @return true if this detector can process the metric
     */
    default boolean supports(String metricName) {
        return true;
    }
}