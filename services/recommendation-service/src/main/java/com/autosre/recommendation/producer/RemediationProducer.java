package com.autosre.recommendation.producer;

import com.autosre.recommendation.gate.ApprovalGate.RemediationPlanWrapper;
import com.autosre.recommendation.model.RemediationRecommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes approved remediation recommendations to the Kafka remediation topic.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public class RemediationProducer {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationProducer.class);

    private final KafkaTemplate<String, RemediationRecommendation> kafkaTemplate;
    private final String remediationTopic;

    public RemediationProducer(
            KafkaTemplate<String, RemediationRecommendation> kafkaTemplate,
            @Value("${autosre.kafka.topics.remediation:autosre.actions.remediation}") String remediationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.remediationTopic = remediationTopic;
    }

    /**
     * Publishes an approved remediation recommendation to Kafka.
     *
     * @param plan the plan wrapper with approval metadata
     */
    public void publishApproved(RemediationPlanWrapper plan) {
        RemediationRecommendation recommendation = RemediationRecommendation.create(
                plan.planId(),
                plan.agentId(),
                plan.alertId(),
                plan.serviceId(),
                parseActions(plan.actionsJson()),
                plan.tier(),
                plan.riskLevel(),
                plan.confidenceScore(),
                "system"
        );

        publish(recommendation);
    }

    /**
     * Publishes a recommendation directly.
     *
     * @param recommendation the recommendation to publish
     */
    public void publish(RemediationRecommendation recommendation) {
        LOG.info("Publishing recommendation: planId={}, agent={}, service={}",
                recommendation.planId(), recommendation.agentId(), recommendation.serviceId());

        String key = recommendation.serviceId();
        CompletableFuture<SendResult<String, RemediationRecommendation>> future =
                kafkaTemplate.send(remediationTopic, key, recommendation);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                LOG.error("Failed to publish recommendation {}: {}",
                        recommendation.planId(), ex.getMessage(), ex);
            } else {
                LOG.info("Recommendation {} published to topic {} partition {} offset {}",
                        recommendation.planId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Parses the JSON actions string into a list of Action objects.
     */
    private List<RemediationRecommendation.Action> parseActions(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return List.of();
        }

        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(actionsJson,
                    mapper.getTypeFactory().constructCollectionType(
                            List.class, RemediationRecommendation.Action.class));
        } catch (Exception e) {
            LOG.warn("Failed to parse actions JSON: {}", e.getMessage());
            return List.of();
        }
    }
}