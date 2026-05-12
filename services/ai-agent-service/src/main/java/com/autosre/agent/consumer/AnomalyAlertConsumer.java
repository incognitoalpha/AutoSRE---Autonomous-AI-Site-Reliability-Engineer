package com.autosre.agent.consumer;

import com.autosre.agent.agent.AgentOrchestrator;
import com.autosre.agent.agent.RootCauseAgent;
import com.autosre.common.model.AnomalyAlert;
import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RootCauseAnalysis;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for anomaly alerts from the anomaly-detection-service
 * and triggers root cause analysis via the RootCauseAgent, then orchestrates
 * specialist agents to produce remediation plans.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RootCauseAgent
 * @see AgentOrchestrator
 */
@Component
public class AnomalyAlertConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AnomalyAlertConsumer.class);

    private final RootCauseAgent rootCauseAgent;
    private final AgentOrchestrator agentOrchestrator;

    public AnomalyAlertConsumer(
            RootCauseAgent rootCauseAgent,
            AgentOrchestrator agentOrchestrator) {
        this.rootCauseAgent = rootCauseAgent;
        this.agentOrchestrator = agentOrchestrator;
    }

    /**
     * Consumes anomaly alerts from Kafka and triggers root cause analysis,
     * then orchestrates specialist agents for remediation planning.
     *
     * @param alert the full AnomalyAlert event published by anomaly-detection-service
     */
    @KafkaListener(
            topics = "${autosre.kafka.topics.anomalies}",
            groupId = "${autosre.kafka.consumer-groups.agent-service}",
            containerFactory = "anomalyAlertListenerContainerFactory"
    )
    public void consume(AnomalyAlert alert) {
        LOG.info("Received anomaly alert: alertId={}, service={}, severity={}, metric={}",
                alert.alertId(), alert.serviceId(), alert.severity(), alert.metricName());

        try {
            // Step 1: Root Cause Analysis
            RootCauseAnalysis rca = rootCauseAgent.analyze(
                    alert.alertId().toString(),
                    alert.serviceId(),
                    alert.severity(),
                    String.format("[%s] %s (value=%.2f)", alert.detectorType(), alert.metricName(), alert.metricValue())
            );

            LOG.info("RCA completed for alertId={}: rootCause={}, confidence={}",
                    alert.alertId(), rca.rootCause(), rca.confidenceScore());

            // Step 2: Orchestrate specialist agents
            List<RemediationPlan> plans = agentOrchestrator.orchestrate(
                    alert.alertId().toString(),
                    alert.serviceId(),
                    alert.severity(),
                    alert.detectorType(),
                    String.format("[%s] %s (value=%.2f)", alert.detectorType(), alert.metricName(), alert.metricValue()),
                    rca.rootCause()
            );

            LOG.info("Orchestration complete for alertId={}: {} remediation plans generated",
                    alert.alertId(), plans.size());

            // Log each plan for monitoring
            for (RemediationPlan plan : plans) {
                LOG.info("Plan: agent={}, planId={}, actions={}, risk={}",
                        plan.agentId(), plan.planId(), plan.actions().size(), plan.riskLevel());
            }

        } catch (Exception e) {
            LOG.error("Failed to process anomaly alert for alertId={}", alert.alertId(), e);
            throw e;
        }
    }
}