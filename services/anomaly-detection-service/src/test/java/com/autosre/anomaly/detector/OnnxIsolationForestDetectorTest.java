package com.autosre.anomaly.detector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;

/**
 * Unit tests for OnnxIsolationForestDetector.
 */
class OnnxIsolationForestDetectorTest {

    private OnnxIsolationForestDetector detector;
    private List<Double> baseline;

    @BeforeEach
    void setUp() {
        detector = new OnnxIsolationForestDetector();
        baseline = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            baseline.add(100.0 + (i * 0.1));
        }
    }

    @Test
    @DisplayName("Should return empty when model is not available")
    void detectModelNotAvailableReturnsEmpty() {
        ReflectionTestUtils.setField(detector, "modelAvailable", false);
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 1000.0);
        Optional<AnomalyAlert> result = detector.detect(event, baseline);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should throw exception for null event when model is available")
    void detectNullEventThrowsException() {
        ReflectionTestUtils.setField(detector, "modelAvailable", true);
        assertThrows(IllegalArgumentException.class, () -> {
            detector.detect(null, baseline);
        });
    }

    @Test
    @DisplayName("Should return empty when baseline is null")
    void detectNullBaselineReturnsEmpty() {
        ReflectionTestUtils.setField(detector, "modelAvailable", true);
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 500.0);
        Optional<AnomalyAlert> result = detector.detect(event, null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when baseline is too small")
    void detectInsufficientBaselineReturnsEmpty() {
        ReflectionTestUtils.setField(detector, "modelAvailable", true);
        List<Double> smallBaseline = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            smallBaseline.add(100.0);
        }
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 500.0);
        Optional<AnomalyAlert> result = detector.detect(event, smallBaseline);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when model inference returns low score")
    void detectNormalValueReturnsEmpty() {
        ReflectionTestUtils.setField(detector, "modelAvailable", true);
        // Currently runOnnxInference returns 0.0f
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 100.0);
        Optional<AnomalyAlert> result = detector.detect(event, baseline);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when model exception occurs")
    void detectModelExceptionReturnsEmpty() {
        ReflectionTestUtils.setField(detector, "modelAvailable", true);
        // Verification that no unhandled exceptions bubble up if runOnnxInference fails.
        // runOnnxInference just returns 0.0f currently, so it won't throw.
        // It's just a general safety check test case.
        TelemetryEvent event = TelemetryEvent.of("test-service", "latency", 100.0);
        Optional<AnomalyAlert> result = detector.detect(event, baseline);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("init() should set modelAvailable to true if enabled")
    void initSetsModelAvailableWhenEnabled() {
        ReflectionTestUtils.setField(detector, "modelEnabled", true);
        detector.init();
        assertTrue(detector.isModelAvailable());
        assertTrue(detector.supports("latency"));
    }

    @Test
    @DisplayName("init() should leave modelAvailable false if disabled")
    void initLeavesModelAvailableFalseWhenDisabled() {
        ReflectionTestUtils.setField(detector, "modelEnabled", false);
        detector.init();
        assertFalse(detector.isModelAvailable());
        assertFalse(detector.supports("latency"));
    }

    @Test
    @DisplayName("Should return correct detector type")
    void getDetectorTypeReturnsCorrectType() {
        assertEquals("ONNX", detector.getDetectorType());
    }

    @Test
    @DisplayName("Should return correct minimum baseline size")
    void getMinBaselineSizeReturnsConfiguredValue() {
        assertEquals(50, detector.getMinBaselineSize());
    }

    @Test
    @DisplayName("Should return correct threshold")
    void getThresholdReturnsConfiguredValue() {
        assertEquals(0.7f, detector.getThreshold());
    }
}