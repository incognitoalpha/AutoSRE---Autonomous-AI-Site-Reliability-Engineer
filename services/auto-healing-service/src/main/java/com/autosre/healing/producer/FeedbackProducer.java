package com.autosre.healing.producer;

import com.autosre.healing.model.HealingOutcomeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes healing outcome events back to Kafka for the feedback loop.
 * These events contain pre/post metric values and success indicators.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Component
public class FeedbackProducer {

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${autosre.kafka.topics.feedback:autosre.telemetry.metrics}")
    private String feedbackTopic;

    public FeedbackProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes a healing outcome event to the feedback topic.
     *
     * @param outcome the outcome event to publish
     */
    public void publish(HealingOutcomeEvent outcome) {
        try {
            String json = objectMapper.writeValueAsString(outcome);
            kafkaTemplate.send(feedbackTopic, outcome.planId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            LOG.error("Failed to publish feedback: planId={}", outcome.planId(), ex);
                        } else {
                            LOG.debug("Feedback published: planId={}, partition={}, offset={}",
                                    outcome.planId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            LOG.error("Failed to serialize healing outcome: {}", outcome.planId(), e);
        }
    }
}