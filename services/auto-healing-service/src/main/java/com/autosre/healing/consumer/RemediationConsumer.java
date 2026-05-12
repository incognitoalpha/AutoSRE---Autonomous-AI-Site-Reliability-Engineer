package com.autosre.healing.consumer;

import com.autosre.healing.audit.AuditLogService;
import com.autosre.healing.executor.HealingActionExecutor;
import com.autosre.healing.model.HealingOutcomeEvent;
import com.autosre.healing.model.RemediationAction;
import com.autosre.healing.producer.FeedbackProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes remediation actions from Kafka and executes them.
 * After execution, persists audit LOG and publishes feedback to Kafka.
 *
 * <p>Bounded context: {@code auto-healing-service}</p>
 */
@Component
public class RemediationConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationConsumer.class);

    private final Map<String, HealingActionExecutor> executorRegistry;
    private final AuditLogService auditLogService;
    private final FeedbackProducer feedbackProducer;
    private final ObjectMapper objectMapper;

    public RemediationConsumer(
            Map<String, HealingActionExecutor> executorRegistry,
            AuditLogService auditLogService,
            FeedbackProducer feedbackProducer,
            ObjectMapper objectMapper) {
        this.executorRegistry = executorRegistry;
        this.auditLogService = auditLogService;
        this.feedbackProducer = feedbackProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes remediation actions from Kafka and executes them.
     *
     * @param message the JSON message containing the remediation action
     */
    @KafkaListener(
            topics = "${autosre.kafka.topics.remediation:autosre.actions.remediation}",
            groupId = "${autosre.kafka.consumer-groups.healing-service:auto-healing-consumer-group}",
            containerFactory = "remediationListenerFactory"
    )
    public void consume(String message) {
        LOG.info("Received remediation action message");
        RemediationAction action;
        try {
            action = objectMapper.readValue(message, RemediationAction.class);
        } catch (Exception e) {
            LOG.error("Failed to deserialize remediation action: {}", message, e);
            return;
        }

        LOG.info("Processing planId={}, actionType={}, confidenceScore={}",
                action.planId(), action.approvalTier(), action.confidenceScore());

        for (RemediationAction.Action nextAction : action.actions()) {
            executeAction(action.planId(), nextAction);
        }
    }

    private void executeAction(String planId, RemediationAction.Action action) {
        String actionType = action.type();
        HealingActionExecutor executor = executorRegistry.get(actionType);

        if (executor == null) {
            LOG.warn("No executor found for action type: {}. Available types: {}",
                    actionType, executorRegistry.keySet());
            HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                    .planId(planId)
                    .actionType(actionType)
                    .target(action.target())
                    .success(false)
                    .executedAt(System.currentTimeMillis())
                    .errorMessage("Unknown action type: " + actionType)
                    .build();
            publishFeedback(outcome);
            return;
        }

        LOG.info("Routing action: type={}, target={}", actionType, action.target());
        try {
            HealingOutcomeEvent outcome = executor.execute(action);
            auditLogService.logAction(
                    planId,
                    actionType,
                    action.target(),
                    outcome.success() ? "SUCCESS" : "FAILURE",
                    outcome.durationMs(),
                    outcome.errorMessage()
            );
            publishFeedback(outcome);
        } catch (Exception e) {
            LOG.error("Executor failed: type={}, target={}", actionType, action.target(), e);
            HealingOutcomeEvent outcome = HealingOutcomeEvent.builder()
                    .planId(planId)
                    .actionType(actionType)
                    .target(action.target())
                    .success(false)
                    .executedAt(System.currentTimeMillis())
                    .durationMs(0)
                    .errorMessage(e.getMessage())
                    .build();
            auditLogService.logAction(planId, actionType, action.target(), "ERROR",
                    0, e.getMessage());
            publishFeedback(outcome);
        }
    }

    private void publishFeedback(HealingOutcomeEvent outcome) {
        try {
            feedbackProducer.publish(outcome);
            LOG.info("Feedback published: planId={}, success={}",
                    outcome.planId(), outcome.success());
        } catch (Exception e) {
            LOG.error("Failed to publish feedback: planId={}", outcome.planId(), e);
        }
    }
}