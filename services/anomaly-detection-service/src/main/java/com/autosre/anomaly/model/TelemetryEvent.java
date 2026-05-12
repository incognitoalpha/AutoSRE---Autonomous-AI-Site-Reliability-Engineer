package com.autosre.anomaly.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable record representing a telemetry event from distributed systems.
 * Events are consumed from Kafka topics and processed for anomaly detection.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 *
 * @param eventId unique identifier for this event
 * @param serviceId the service generating the telemetry
 * @param metricName the name of the metric (e.g., "http_request_latency_ms")
 * @param value the numeric value of the metric
 * @param timestamp when the metric was captured
 * @param labels additional labels/tags for the metric
 * @param traceId OpenTelemetry trace identifier
 * @param spanId OpenTelemetry span identifier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryEvent(
    UUID eventId,
    String serviceId,
    String metricName,
    double value,
    Instant timestamp,
    Map<String, String> labels,
    String traceId,
    String spanId
) {
    public TelemetryEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (serviceId == null || serviceId.isBlank()) {
            throw new IllegalArgumentException("serviceId must not be null or blank");
        }
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("metricName must not be null or blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (labels == null) {
            labels = Map.of();
        }
    }

    /**
     * Factory method to create a TelemetryEvent from raw metric data.
     *
     * @param serviceId the service identifier
     * @param metricName the metric name
     * @param value the metric value
     * @return a new TelemetryEvent with generated ID and current timestamp
     */
    public static TelemetryEvent of(String serviceId, String metricName, double value) {
        return new TelemetryEvent(UUID.randomUUID(), serviceId, metricName, value, Instant.now(), Map.of(), null, null);
    }

    /**
     * Returns true if this event contains an error rate metric.
     *
     * @return true if metric name contains "error"
     */
    public boolean isErrorRateMetric() {
        return metricName != null && metricName.toLowerCase().contains("error");
    }

    /**
     * Returns true if this event contains a latency metric.
     *
     * @return true if metric name contains "latency" or "duration"
     */
    public boolean isLatencyMetric() {
        return metricName != null &&
            (metricName.toLowerCase().contains("latency") || metricName.toLowerCase().contains("duration"));
    }

    /**
     * Returns a label value or empty string if not present.
     *
     * @param key the label key
     * @return the label value or empty string
     */
    public String getLabel(String key) {
        return labels != null ? labels.getOrDefault(key, "") : "";
    }
}