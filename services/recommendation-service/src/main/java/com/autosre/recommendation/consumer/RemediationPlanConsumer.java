package com.autosre.recommendation.consumer;

import com.autosre.recommendation.gate.ApprovalGate;
import com.autosre.recommendation.gate.ApprovalGate.ApprovalDecision;
import com.autosre.recommendation.gate.ApprovalGate.RemediationPlanWrapper;
import com.autosre.recommendation.scoring.ConfidenceScoringService;
import com.autosre.recommendation.scoring.RiskLevelClassifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/**
 * Kafka consumer that receives RemediationPlan messages from the ai-agent-service
 * and routes them through the appropriate approval gate.
 *
 * <p>Bounded context: {@code recommendation-service}</p>
 */
@Component
public class RemediationPlanConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationPlanConsumer.class);

    private final ConfidenceScoringService scoringService;
    private final RiskLevelClassifier riskClassifier;
    private final ApprovalGate autoGate;
    private final ApprovalGate asyncGate;
    private final ApprovalGate syncGate;
    private final ObjectMapper objectMapper;

    public RemediationPlanConsumer(
            ConfidenceScoringService scoringService,
            RiskLevelClassifier riskClassifier,
            ApprovalGate autoGate,
            ApprovalGate asyncGate,
            ApprovalGate syncGate,
            ObjectMapper objectMapper) {
        this.scoringService = scoringService;
        this.riskClassifier = riskClassifier;
        this.autoGate = autoGate;
        this.asyncGate = asyncGate;
        this.syncGate = syncGate;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes remediation plans from Kafka and routes through approval gates.
     *
     * @param plan the remediation plan JSON
     */
    @KafkaListener(
            topics = "${autosre.kafka.topics.anomalies}",
            groupId = "${autosre.kafka.consumer-groups.recommendation-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(RemediationPlanMessage plan) {
        LOG.info("Received remediation plan: planId={}, agent={}, service={}",
                plan.planId(), plan.agentId(), plan.serviceId());

        try {
            processPlan(plan);
        } catch (Exception e) {
            LOG.error("Failed to process plan {}: {}", plan.planId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Processes a remediation plan through the approval pipeline.
     */
    private void processPlan(RemediationPlanMessage plan) {
        Set<String> actionTypes = extractActionTypes(plan.actionsJson());
        var riskLevel = riskClassifier.classify(
                plan.planId().toString(), actionTypes, 1);

        double adjustedConfidence = scoringService.computeScore(
                plan.confidenceScore(),
                plan.rootCause(),
                plan.hasRunbookMatch(),
                plan.isCritical(),
                plan.similarIncidentCount()
        );

        RemediationPlanWrapper wrapper = new RemediationPlanWrapper(
                plan.planId(),
                plan.agentId(),
                plan.alertId(),
                plan.serviceId(),
                plan.actionsJson(),
                riskLevel,
                adjustedConfidence,
                null
        );

        ApprovalGate gate = selectGate(riskLevel);
        ApprovalDecision decision = gate.process(wrapper);

        LOG.info("Plan {} routed to {}: decision={}",
                plan.planId(), gate.getClass().getSimpleName(), decision);
    }

    /**
     * Selects the appropriate approval gate based on risk level.
     */
    private ApprovalGate selectGate(com.autosre.recommendation.model.RemediationRecommendation.RiskLevel riskLevel) {
        if (autoGate.supports(riskLevel)) {
            return autoGate;
        }
        if (asyncGate.supports(riskLevel)) {
            return asyncGate;
        }
        return syncGate;
    }

    /**
     * Extracts action types from the actions JSON.
     */
    private Set<String> extractActionTypes(String actionsJson) {
        if (actionsJson == null || actionsJson.isBlank()) {
            return Set.of();
        }

        try {
            var node = objectMapper.readTree(actionsJson);
            Set<String> types = new java.util.HashSet<>();
            if (node.isArray()) {
                for (var item : node) {
                    if (item.has("actionType")) {
                        types.add(item.get("actionType").asText());
                    }
                }
            }
            return types;
        } catch (Exception e) {
            LOG.warn("Failed to parse action types: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Message wrapper for Kafka deserialization.
     */
    public record RemediationPlanMessage(
            UUID planId,
            String agentId,
            String alertId,
            String serviceId,
            String actionsJson,
            double confidenceScore,
            String rootCause,
            boolean hasRunbookMatch,
            boolean isCritical,
            int similarIncidentCount
    ) {
    }
}