package com.autosre.anomaly.detector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;

/**
 * Unit tests for ZScoreDetector.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
class ZScoreDetectorTest {

    private ZScoreDetector detector;
    private List<Double> baseline;

    @BeforeEach
    void setUp() {
        detector = new ZScoreDetector();
        detector.setThreshold(2.0);
        detector.setMinBaselineSize(30);

        baseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            baseline.add(100.0 + (i * 0.1));
        }
    }

    @Test
    @DisplayName("Should detect anomaly when value exceeds threshold")
    void detectAnomalyAboveThresholdReturnsAlert() {
        TelemetryEvent event = new TelemetryEvent(
            UUID.randomUUID(),
            "test-service",
            "http_request_latency_ms",
            1000.0,
            Instant.now(),
            java.util.Map.of(),
            null,
            null
        );

        Optional<AnomalyAlert> result = detector.detect(event, baseline);

        assertTrue(result.isPresent());
        AnomalyAlert alert = result.get();
        assertEquals("test-service", alert.serviceId());
        assertEquals("ZSCORE", alert.detectorType());
    }

    @Test
    @DisplayName("Should return empty when value is within threshold")
    void detectNormalValueReturnsEmpty() {
        double mean = baseline.stream().mapToDouble(Double::doubleValue).average().orElse(100);
        TelemetryEvent event = new TelemetryEvent(
            UUID.randomUUID(),
            "test-service",
            "http_request_latency_ms",
            mean,
            Instant.now(),
            java.util.Map.of(),
            null,
            null
        );

        Optional<AnomalyAlert> result = detector.detect(event, baseline);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when baseline is too small")
    void detectInsufficientBaselineReturnsEmpty() {
        List<Double> smallBaseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            smallBaseline.add(100.0);
        }

        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 500.0);

        Optional<AnomalyAlert> result = detector.detect(event, smallBaseline);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for null event")
    void detectNullEventThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            detector.detect(null, baseline);
        });
    }

    @Test
    @DisplayName("Should return correct detector type")
    void getDetectorTypeReturnsCorrectType() {
        assertEquals("ZSCORE", detector.getDetectorType());
    }

    @Test
    @DisplayName("Should return correct threshold")
    void getThresholdReturnsConfiguredValue() {
        assertEquals(2.0, detector.getThreshold());
    }

    @Test
    @DisplayName("Should return correct minimum baseline size")
    void getMinBaselineSizeReturnsConfiguredValue() {
        assertEquals(30, detector.getMinBaselineSize());
    }

    @Test
    @DisplayName("Should detect anomaly for high z-score")
    void detectHighZScoreReturnsAnomaly() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 1000.0);

        Optional<AnomalyAlert> result = detector.detect(event, baseline);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should reject invalid threshold value")
    void setThresholdInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            detector.setThreshold(-1.0);
        });
    }

    @Test
    @DisplayName("Should detect anomaly for constant baseline with deviating value")
    void detectConstantBaselineWithDeviatingValueReturnsAlert() {
        List<Double> constantBaseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            constantBaseline.add(100.0);
        }

        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 500.0);

        Optional<AnomalyAlert> result = detector.detect(event, constantBaseline);

        assertTrue(result.isPresent());
        assertEquals("test-service", result.get().serviceId());
        assertEquals("ZSCORE", result.get().detectorType());
    }

    @Test
    @DisplayName("Should return empty for constant baseline with matching value")
    void detectConstantBaselineWithMatchingValueReturnsEmpty() {
        List<Double> constantBaseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            constantBaseline.add(100.0);
        }

        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 100.0);

        Optional<AnomalyAlert> result = detector.detect(event, constantBaseline);

        assertTrue(result.isEmpty());
    }
}