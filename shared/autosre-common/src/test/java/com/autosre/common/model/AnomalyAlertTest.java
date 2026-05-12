package com.autosre.common.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for AnomalyAlert record.
 *
 * <p>Bounded context: {@code shared module}</p>
 */
class AnomalyAlertTest {

    @Test
    @DisplayName("Should create AnomalyAlert with critical factory method")
    void criticalCreatesAlertWithCriticalSeverity() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 95.0, 15.0);

        AnomalyAlert alert = AnomalyAlert.critical("payment-service", "error_rate", 0.15, stats, "ZSCORE");

        assertNotNull(alert.alertId());
        assertEquals("payment-service", alert.serviceId());
        assertEquals(Severity.CRITICAL, alert.severity());
        assertEquals("error_rate", alert.metricName());
        assertEquals(0.15, alert.metricValue());
        assertEquals("ZSCORE", alert.detectorType());
        assertTrue(alert.confidenceScore() > 0.9);
    }

    @Test
    @DisplayName("Should create AnomalyAlert with high factory method")
    void highCreatesAlertWithHighSeverity() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 95.0, 15.0);

        AnomalyAlert alert = AnomalyAlert.high("order-service", "latency", 250.0, stats, "MAD");

        assertEquals(Severity.HIGH, alert.severity());
        assertEquals("MAD", alert.detectorType());
        assertTrue(alert.confidenceScore() >= 0.5);
    }

    @Test
    @DisplayName("Should generate alert summary string")
    void toAlertSummaryFormatsCorrectly() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 0, 0);
        AnomalyAlert alert = new AnomalyAlert(
            UUID.randomUUID(),
            "test-service",
            Severity.HIGH,
            "latency",
            200.0,
            stats,
            "ZSCORE",
            0.85,
            Instant.now(),
            "trace-123",
            List.of("test-service")
        );

        String summary = alert.toAlertSummary();

        assertTrue(summary.contains("test-service"));
        assertTrue(summary.contains("HIGH"));
        assertTrue(summary.contains("latency"));
        assertTrue(summary.contains("200"));
    }

    @Test
    @DisplayName("Should reject null serviceId")
    void constructorNullServiceIdThrowsException() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 0, 0);

        assertThrows(IllegalArgumentException.class, () -> {
            new AnomalyAlert(UUID.randomUUID(), null, Severity.HIGH, "metric", 100.0, stats, "TEST", 0.5, Instant.now(), null, List.of());
        });
    }

    @Test
    @DisplayName("Should default severity to HIGH when null")
    void constructorNullSeverityDefaultsToHigh() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 0, 0);

        AnomalyAlert alert = new AnomalyAlert(
            UUID.randomUUID(),
            "service",
            null,
            "metric",
            100.0,
            stats,
            "TEST",
            0.5,
            Instant.now(),
            null,
            List.of()
        );

        assertEquals(Severity.HIGH, alert.severity());
    }

    @Test
    @DisplayName("Should clamp confidence score to valid range")
    void constructorInvalidConfidenceClamped() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 0, 0);

        AnomalyAlert alert1 = new AnomalyAlert(
            UUID.randomUUID(), "service", Severity.HIGH, "metric", 100.0, stats, "TEST",
            -0.5, Instant.now(), null, List.of()
        );

        assertEquals(0.5, alert1.confidenceScore());

        AnomalyAlert alert2 = new AnomalyAlert(
            UUID.randomUUID(), "service", Severity.HIGH, "metric", 100.0, stats, "TEST",
            1.5, Instant.now(), null, List.of()
        );

        assertEquals(0.5, alert2.confidenceScore());
    }

    @Test
    @DisplayName("Should create baseline stats correctly")
    void baselineStatsContainsCorrectValues() {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(100.0, 10.0, 95.0, 15.0);

        assertEquals(100.0, stats.mean());
        assertEquals(10.0, stats.stdDev());
        assertEquals(95.0, stats.median());
        assertEquals(15.0, stats.mad());
    }
}
