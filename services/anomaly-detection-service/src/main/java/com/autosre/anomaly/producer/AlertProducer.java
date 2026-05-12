package com.autosre.anomaly.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import com.autosre.common.model.AnomalyAlert;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes anomaly alerts to the Kafka anomalies topic.
 * Ensures structured JSON output and proper trace context propagation.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@Component
public class AlertProducer {

    private static final Logger LOG = LoggerFactory.getLogger(AlertProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String anomalyTopic;

    public AlertProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${autosre.kafka.topics.anomalies}") String anomalyTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.anomalyTopic = anomalyTopic;
    }

    /**
     * Publishes an anomaly alert to Kafka asynchronously.
     *
     * @param alert the anomaly alert to publish
     */
    public void publishAlert(AnomalyAlert alert) {
        LOG.info("Publishing anomaly alert: alertId={}, service={}, severity={}, metric={}",
            alert.alertId(), alert.serviceId(), alert.severity(), alert.metricName());

        try {
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(anomalyTopic, alert.serviceId(), alert);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    LOG.error("Failed to publish alert {}: {}",
                        alert.alertId(), ex.getMessage(), ex);
                } else {
                    LOG.debug("Alert {} published to partition {} at offset {}",
                        alert.alertId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            LOG.error("Error sending alert to Kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish anomaly alert", e);
        }
    }

    /**
     * Publishes an anomaly alert synchronously (blocking).
     * Use this when confirmation is required before proceeding.
     *
     * @param alert the anomaly alert to publish
     * @throws RuntimeException if publishing fails
     */
    public void publishAlertSync(AnomalyAlert alert) {
        LOG.info("Publishing anomaly alert (sync): alertId={}, service={}, severity={}",
            alert.alertId(), alert.serviceId(), alert.severity());

        try {
            SendResult<String, Object> result =
                kafkaTemplate.send(anomalyTopic, alert.serviceId(), alert).get();

            LOG.info("Alert {} published successfully to partition {} at offset {}",
                alert.alertId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
        } catch (Exception e) {
            LOG.error("Failed to publish alert {} synchronously: {}",
                alert.alertId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish anomaly alert synchronously", e);
        }
    }
}