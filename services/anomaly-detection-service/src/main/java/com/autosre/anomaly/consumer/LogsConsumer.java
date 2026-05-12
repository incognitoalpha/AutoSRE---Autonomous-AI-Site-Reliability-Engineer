package com.autosre.anomaly.consumer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.autosre.common.model.AnomalyAlert;
import com.autosre.anomaly.model.TelemetryEvent;
import com.autosre.anomaly.producer.AlertProducer;
import com.autosre.common.model.Severity;

/**
 * Consumes telemetry logs from Kafka and detects error patterns.
 * Specifically looks for error keywords and thresholds in log messages.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@Component
public class LogsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(LogsConsumer.class);

    private static final List<Pattern> ERROR_PATTERNS = List.of(
        Pattern.compile("(?i)error", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)exception", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)failed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)fatal", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)timeout", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?i)out of memory", Pattern.CASE_INSENSITIVE)
    );

    private static final Map<String, Integer> ERROR_COUNTS = new ConcurrentHashMap<>();
    private static final int ERROR_THRESHOLD = 10;
    private static final long WINDOW_MS = 60_000;

    private final AlertProducer alertProducer;

    public LogsConsumer(AlertProducer alertProducer) {
        this.alertProducer = alertProducer;
    }

    @KafkaListener(
        topics = "${autosre.kafka.topics.logs}",
        groupId = "${autosre.kafka.consumer-groups.logs-consumer}",
        containerFactory = "logsListenerContainerFactory"
    )
    public void consume(TelemetryEvent event) {
        LOG.debug("Received log event: service={}, level={}, message={}",
            event.serviceId(), event.getLabel("level"), event.getLabel("message"));

        try {
            String message = event.getLabel("message");
            String level = event.getLabel("level").toUpperCase();

            if (isErrorLevel(level) || containsErrorPattern(message)) {
                incrementErrorCount(event.serviceId());

                int errorCount = ERROR_COUNTS.getOrDefault(event.serviceId(), 0);
                if (errorCount >= ERROR_THRESHOLD) {
                    publishAnomalyAlert(event, errorCount);
                    ERROR_COUNTS.put(event.serviceId(), 0);
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing log event for service {}: {}",
                event.serviceId(), e.getMessage(), e);
        }
    }

    private boolean isErrorLevel(String level) {
        return "ERROR".equals(level) || "FATAL".equals(level) || "WARN".equals(level);
    }

    private boolean containsErrorPattern(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return ERROR_PATTERNS.stream()
            .anyMatch(pattern -> pattern.matcher(message).find());
    }

    private void incrementErrorCount(String serviceId) {
        ERROR_COUNTS.merge(serviceId, 1, Integer::sum);
    }

    private void publishAnomalyAlert(TelemetryEvent event, int errorCount) {
        AnomalyAlert.BaselineStats stats = new AnomalyAlert.BaselineStats(
            0, 0, 0, 0
        );

        AnomalyAlert alert = new AnomalyAlert(
            java.util.UUID.randomUUID(),
            event.serviceId(),
            Severity.HIGH,
            "error_rate",
            errorCount,
            stats,
            "LOG_PATTERN",
            0.75,
            java.time.Instant.now(),
            event.traceId(),
            List.of(event.serviceId())
        );

        LOG.info("Log-based anomaly detected: service={}, errorCount={}", event.serviceId(), errorCount);
        alertProducer.publishAlert(alert);
    }

    public Map<String, Integer> getCurrentErrorCounts() {
        return new ConcurrentHashMap<>(ERROR_COUNTS);
    }

    public void resetErrorCounts() {
        ERROR_COUNTS.clear();
    }
}