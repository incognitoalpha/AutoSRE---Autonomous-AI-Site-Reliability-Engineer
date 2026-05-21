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
 * Unit tests for MadDetector.
 */
class MadDetectorTest {

    private MadDetector detector;
    private List<Double> baseline;
    private List<Double> constantBaseline;

    @BeforeEach
    void setUp() {
        detector = new MadDetector();
        detector.setThreshold(2.0);
        detector.setMinBaselineSize(30);

        baseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            baseline.add(100.0 + (i % 5)); // 100, 101, 102, 103, 104 ... -> non-zero MAD
        }
        
        constantBaseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            constantBaseline.add(100.0);
        }
    }

    @Test
    @DisplayName("Should detect anomaly when value exceeds threshold")
    void detectAnomalyAboveThresholdReturnsAlert() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "memory", 1000.0);
        Optional<AnomalyAlert> result = detector.detect(event, baseline);
        assertTrue(result.isPresent());
        assertEquals("MAD", result.get().detectorType());
    }

    @Test
    @DisplayName("Should return empty when value is within threshold")
    void detectNormalValueReturnsEmpty() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "memory", 102.0);
        Optional<AnomalyAlert> result = detector.detect(event, baseline);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when baseline is null")
    void detectNullBaselineReturnsEmpty() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "memory", 500.0);
        Optional<AnomalyAlert> result = detector.detect(event, null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when baseline is too small")
    void detectInsufficientBaselineReturnsEmpty() {
        List<Double> smallBaseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            smallBaseline.add(100.0);
        }
        TelemetryEvent event = TelemetryEvent.of("test-service", "memory", 500.0);
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
        assertEquals("MAD", detector.getDetectorType());
    }

    @Test
    @DisplayName("Should return correct threshold")
    void getThresholdReturnsConfiguredValue() {
        assertEquals(2.0, detector.getThreshold());
    }

    @Test
    @DisplayName("Should reject invalid threshold value")
    void setThresholdInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> detector.setThreshold(0.0));
        assertThrows(IllegalArgumentException.class, () -> detector.setThreshold(-1.0));
    }

    @Test
    @DisplayName("Should reject invalid sensitivity value")
    void setSensitivityInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> detector.setSensitivity(0.0));
        assertThrows(IllegalArgumentException.class, () -> detector.setSensitivity(-1.0));
    }

    @Test
    @DisplayName("Should reject invalid baseline size")
    void setMinBaselineSizeInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> detector.setMinBaselineSize(4));
    }

    @Test
    @DisplayName("Should detect anomaly for constant baseline with deviating value")
    void detectConstantBaselineWithDeviatingValueReturnsAlert() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 500.0);
        Optional<AnomalyAlert> result = detector.detect(event, constantBaseline);
        assertTrue(result.isPresent());
        assertEquals("test-service", result.get().serviceId());
        assertEquals("MAD", result.get().detectorType());
    }

    @Test
    @DisplayName("Should detect anomaly for constant zero baseline with positive deviating value")
    void detectConstantZeroBaselineWithPositiveDeviatingValueReturnsAlert() {
        List<Double> zeroBaseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            zeroBaseline.add(0.0);
        }
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 10.0);
        Optional<AnomalyAlert> result = detector.detect(event, zeroBaseline);
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should return empty for constant baseline with matching value")
    void detectConstantBaselineWithMatchingValueReturnsEmpty() {
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 100.0);
        Optional<AnomalyAlert> result = detector.detect(event, constantBaseline);
        assertTrue(result.isEmpty());
    }
}