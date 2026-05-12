package com.autosre.anomaly.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TelemetryEvent record.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
class TelemetryEventTest {

    @Test
    @DisplayName("Should create TelemetryEvent with factory method")
    void ofCreatesValidEvent() {
        TelemetryEvent event = TelemetryEvent.of("payment-service", "http_request_latency_ms", 150.5);

        assertNotNull(event.eventId());
        assertEquals("payment-service", event.serviceId());
        assertEquals("http_request_latency_ms", event.metricName());
        assertEquals(150.5, event.value());
        assertNotNull(event.timestamp());
        assertTrue(event.labels().isEmpty());
    }

    @Test
    @DisplayName("Should reject null serviceId")
    void constructorNullServiceIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TelemetryEvent(UUID.randomUUID(), null, "metric", 100.0, Instant.now(), java.util.Map.of(), null, null);
        });
    }

    @Test
    @DisplayName("Should reject blank serviceId")
    void constructorBlankServiceIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new TelemetryEvent(UUID.randomUUID(), "  ", "metric", 100.0, Instant.now(), java.util.Map.of(), null, null);
        });
    }

    @Test
    @DisplayName("Should identify error rate metrics")
    void isErrorRateMetricDetectsErrorMetrics() {
        TelemetryEvent errorEvent = TelemetryEvent.of("service", "error_rate", 0.05);
        TelemetryEvent latencyEvent = TelemetryEvent.of("service", "latency", 100.0);

        assertTrue(errorEvent.isErrorRateMetric());
        assertFalse(latencyEvent.isErrorRateMetric());
    }

    @Test
    @DisplayName("Should identify latency metrics")
    void isLatencyMetricDetectsLatencyMetrics() {
        TelemetryEvent latencyEvent = TelemetryEvent.of("service", "http_request_latency_ms", 100.0);
        TelemetryEvent durationEvent = TelemetryEvent.of("service", "db_query_duration_ms", 50.0);
        TelemetryEvent countEvent = TelemetryEvent.of("service", "request_count", 1000.0);

        assertTrue(latencyEvent.isLatencyMetric());
        assertTrue(durationEvent.isLatencyMetric());
        assertFalse(countEvent.isLatencyMetric());
    }

    @Test
    @DisplayName("Should return label value or empty string")
    void getLabelReturnsValueOrEmpty() {
        TelemetryEvent event = new TelemetryEvent(
            UUID.randomUUID(),
            "service",
            "metric",
            100.0,
            Instant.now(),
            java.util.Map.of("env", "prod", "region", "us-east-1"),
            null,
            null
        );

        assertEquals("prod", event.getLabel("env"));
        assertEquals("us-east-1", event.getLabel("region"));
        assertEquals("", event.getLabel("missing"));
    }

    @Test
    @DisplayName("Should default to empty labels when null")
    void constructorNullLabelsDefaultsToEmpty() {
        TelemetryEvent event = new TelemetryEvent(
            UUID.randomUUID(),
            "service",
            "metric",
            100.0,
            Instant.now(),
            null,
            null,
            null
        );

        assertTrue(event.labels().isEmpty());
        assertEquals("", event.getLabel("any"));
    }
}