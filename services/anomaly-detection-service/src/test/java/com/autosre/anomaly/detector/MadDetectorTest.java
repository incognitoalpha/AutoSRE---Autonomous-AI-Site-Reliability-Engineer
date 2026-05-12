package com.autosre.anomaly.detector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;

/**
 * Unit tests for MadDetector.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
class MadDetectorTest {

    private MadDetector detector;
    private List<Double> baseline;

    @BeforeEach
    void setUp() {
        detector = new MadDetector();
        detector.setThreshold(2.0);
        detector.setMinBaselineSize(30);

        baseline = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            baseline.add(100.0);
        }
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
        assertThrows(IllegalArgumentException.class, () -> {
            detector.setThreshold(0.0);
        });
    }

    @Test
    @DisplayName("Should reject invalid baseline size")
    void setMinBaselineSizeInvalidValueThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            detector.setMinBaselineSize(3);
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
        assertEquals("MAD", result.get().detectorType());
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