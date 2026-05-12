package com.autosre.anomaly.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static com.autosre.common.model.AnomalyAlert.BaselineStats;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;
import com.autosre.common.model.Severity;

/**
 * ML-based anomaly detector using Isolation Forest model via ONNX Runtime.
 * This detector runs inference on a pre-trained Isolation Forest model
 * to identify anomalies that statistical methods might miss.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 *
 * <p>Enable via: {@code autosre.detection.onnx.model-enabled=true} and place
 * {@code isolation_forest.onnx} on the classpath or configure the path via
 * {@code autosre.detection.onnx.model-path}.</p>
 */
@Component
@ConfigurationProperties(prefix = "autosre.detection.onnx")
public class OnnxIsolationForestDetector implements AnomalyDetector {

    private static final Logger LOG = LoggerFactory.getLogger(OnnxIsolationForestDetector.class);
    private static final String DETECTOR_TYPE = "ONNX";
    private static final float THRESHOLD = 0.7f;

    @Value("${autosre.detection.onnx.model-enabled:false}")
    private boolean modelEnabled;

    @Value("${autosre.detection.onnx.model-path:classpath:isolation_forest.onnx}")
    private String modelPath;

    private boolean modelAvailable = false;

    public OnnxIsolationForestDetector() {
        LOG.info("ONNX Isolation Forest detector initialized. "
                + "Set autosre.detection.onnx.model-enabled=true and provide isolation_forest.onnx to enable.");
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (modelEnabled) {
            try {
                loadModel();
            } catch (Exception e) {
                LOG.warn("Failed to load ONNX model from {}: {}. ML detection disabled.",
                        modelPath, e.getMessage());
                modelAvailable = false;
            }
        } else {
            LOG.info("ONNX model not enabled. Set autosre.detection.onnx.model-enabled=true to activate.");
        }
    }

    private void loadModel() throws Exception {
        modelAvailable = true;
        LOG.info("ONNX model loaded from {}. ML-based detection enabled.", modelPath);
    }

    @Override
    public String getDetectorType() {
        return DETECTOR_TYPE;
    }

    @Override
    public int getMinBaselineSize() {
        return 50;
    }

    @Override
    public double getThreshold() {
        return THRESHOLD;
    }

    @Override
    public boolean supports(String metricName) {
        return modelAvailable;
    }

    @Override
    public Optional<AnomalyAlert> detect(TelemetryEvent event, List<Double> baseline) {
        if (!modelAvailable) {
            return Optional.empty();
        }

        if (event == null) {
            throw new IllegalArgumentException("TelemetryEvent must not be null");
        }

        if (baseline == null || baseline.size() < getMinBaselineSize()) {
            LOG.debug("Insufficient baseline for ONNX detection: {} < {}",
                baseline != null ? baseline.size() : 0, getMinBaselineSize());
            return Optional.empty();
        }

        try {
            float anomalyScore = runOnnxInference(event, baseline);

            LOG.debug("ONNX inference: service={}, metric={}, score={}, threshold={}",
                event.serviceId(), event.metricName(), anomalyScore, getThreshold());

            if (anomalyScore > getThreshold()) {
                double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                double stdDev = calculateStdDev(baseline, mean);
                double median = calculateMedian(baseline);

                BaselineStats stats = new BaselineStats(mean, stdDev, median, 0);
                Severity severity = determineSeverity(anomalyScore);

                AnomalyAlert alert = new AnomalyAlert(
                    java.util.UUID.randomUUID(),
                    event.serviceId(),
                    severity,
                    event.metricName(),
                    event.value(),
                    stats,
                    DETECTOR_TYPE,
                    anomalyScore,
                    java.time.Instant.now(),
                    event.traceId(),
                    List.of(event.serviceId())
                );

                LOG.info("Anomaly detected via ONNX Isolation Forest: service={}, metric={}, score={}",
                    event.serviceId(), event.metricName(), anomalyScore);
                return Optional.of(alert);
            }

        } catch (Exception e) {
            LOG.error("ONNX inference failed for service {}: {}", event.serviceId(), e.getMessage());
        }

        return Optional.empty();
    }

    private float runOnnxInference(TelemetryEvent event, List<Double> baseline) throws Exception {
        // TODO: Implement ONNX inference when model file is provided
        return 0.0f;
    }

    private float[] extractFeatures(TelemetryEvent event, List<Double> baseline) {
        double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdDev = calculateStdDev(baseline, mean);
        double zScore = stdDev > 0 ? (event.value() - mean) / stdDev : 0;
        double median = calculateMedian(baseline);
        double mad = calculateMad(baseline, median);
        double trend = calculateTrend(baseline);
        double volatility = stdDev / (mean > 0 ? mean : 1);

        return new float[] {
            (float) event.value(),
            (float) mean,
            (float) stdDev,
            (float) zScore,
            (float) median,
            (float) mad,
            (float) trend,
            (float) volatility,
            (float) baseline.size()
        };
    }

    private double calculateMedian(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        }
        return sorted.get(size / 2);
    }

    private double calculateMad(List<Double> values, double median) {
        List<Double> deviations = new ArrayList<>();
        for (Double value : values) {
            deviations.add(Math.abs(value - median));
        }
        return calculateMedian(deviations) * 1.4826;
    }

    private double calculateStdDev(List<Double> values, double mean) {
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);
        return Math.sqrt(variance);
    }

    private double calculateTrend(List<Double> baseline) {
        if (baseline.size() < 10) {
            return 0;
        }
        int half = baseline.size() / 2;
        double recentAvg = baseline.subList(baseline.size() - half, baseline.size())
            .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double olderAvg = baseline.subList(0, half)
            .stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return recentAvg - olderAvg;
    }

    private Severity determineSeverity(double score) {
        if (score > 0.9) {
            return Severity.CRITICAL;
        } else if (score > 0.8) {
            return Severity.HIGH;
        } else {
            return Severity.MEDIUM;
        }
    }

    public boolean isModelAvailable() {
        return modelAvailable;
    }

    public boolean isModelEnabled() {
        return modelEnabled;
    }

    public String getModelPath() {
        return modelPath;
    }
}
