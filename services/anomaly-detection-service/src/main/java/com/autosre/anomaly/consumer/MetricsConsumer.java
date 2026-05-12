package com.autosre.anomaly.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.autosre.anomaly.detector.AnomalyDetector;
import com.autosre.anomaly.model.TelemetryEvent;
import com.autosre.anomaly.producer.AlertProducer;

/**
 * Consumes telemetry metrics from Kafka and runs anomaly detection.
 * Uses a strategy pattern to run multiple detectors and publishes
 * the first anomaly detected.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@Component
public class MetricsConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsConsumer.class);
    private static final int MAX_BASELINE_SIZE = 1000;

    private final List<AnomalyDetector> detectors;
    private final AlertProducer alertProducer;
    private final Map<String, List<Double>> baselines;

    public MetricsConsumer(
            List<AnomalyDetector> detectors,
            AlertProducer alertProducer) {
        this.detectors = detectors;
        this.alertProducer = alertProducer;
        this.baselines = new ConcurrentHashMap<>();
        LOG.info("MetricsConsumer initialized with {} detectors: {}",
            detectors.size(), detectorNames());
    }

    @KafkaListener(
        topics = "${autosre.kafka.topics.metrics}",
        groupId = "${autosre.kafka.consumer-groups.metrics-consumer}",
        containerFactory = "metricsListenerContainerFactory"
    )
    public void consume(TelemetryEvent event) {
        LOG.debug("Received metric event: service={}, metric={}, value={}",
            event.serviceId(), event.metricName(), event.value());

        try {
            updateBaseline(event);
            List<Double> baseline = baselines.get(serviceKey(event));

            for (AnomalyDetector detector : detectors) {
                if (!detector.supports(event.metricName())) {
                    continue;
                }

                if (baseline != null && baseline.size() >= detector.getMinBaselineSize()) {
                    detector.detect(event, baseline).ifPresent(alert -> {
                        LOG.info("Anomaly detected: {}", alert.toAlertSummary());
                        alertProducer.publishAlert(alert);
                    });
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing metric event for service {}: {}",
                event.serviceId(), e.getMessage(), e);
        }
    }

    private void updateBaseline(TelemetryEvent event) {
        String key = serviceKey(event);
        baselines.computeIfAbsent(key, k -> new ArrayList<>());

        List<Double> baseline = baselines.get(key);
        baseline.add(event.value());

        if (baseline.size() > MAX_BASELINE_SIZE) {
            baseline.remove(0);
        }
    }

    private String serviceKey(TelemetryEvent event) {
        return event.serviceId() + ":" + event.metricName();
    }

    private String detectorNames() {
        return String.join(", ", detectors.stream()
            .map(AnomalyDetector::getDetectorType)
            .toList());
    }

    public Map<String, Integer> getBaselineSizes() {
        Map<String, Integer> sizes = new ConcurrentHashMap<>();
        baselines.forEach((key, value) -> sizes.put(key, value.size()));
        return sizes;
    }

    public void clearBaselines() {
        baselines.clear();
    }
}