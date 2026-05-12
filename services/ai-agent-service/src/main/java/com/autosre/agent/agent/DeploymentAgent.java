package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RemediationPlan.ActionType;
import com.autosre.agent.model.RemediationPlan.RemediationAction;
import com.autosre.agent.model.RemediationPlan.RemediationAction.Parameter;
import com.autosre.agent.model.RiskLevel;
import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Specialist agent that detects post-deployment regressions and produces
 * rollback plans. Monitors for error rate spikes, latency increases, or
 * other anomalies that correlate with recent deployment events.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Component
public class DeploymentAgent implements SpecialistAgent {

    private static final Logger LOG = LoggerFactory.getLogger(DeploymentAgent.class);

    public DeploymentAgent() {
    }

    @Override
    public String getAgentName() {
        return "DeploymentAgent";
    }

    @Override
    public boolean isApplicable(String anomalyType, String rootCause) {
        String lowerType = anomalyType.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        return lowerType.contains("deploy") ||
               lowerType.contains("rollback") ||
               lowerType.contains("version") ||
               lowerType.contains("release") ||
               lowerType.contains("regression") ||
               lowerCause.contains("deployment") ||
               lowerCause.contains("version") ||
               lowerCause.contains("release") ||
               lowerCause.contains("post-deploy");
    }

    @Override
    public RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        LOG.info("DeploymentAgent analyzing: alertId={}, service={}, severity={}",
                alertId, serviceId, severity);

        try {
            List<RemediationAction> actions = determineRollbackActions(
                    serviceId, severity, anomalyDescription, rootCause);
            RiskLevel riskLevel = determineRiskLevel(actions, severity);

            RemediationPlan plan = new RemediationPlan(
                    java.util.UUID.randomUUID(),
                    getAgentName(),
                    alertId,
                    serviceId,
                    actions,
                    riskLevel,
                    "Rollback to stable version to resolve regression",
                    Instant.now()
            );

            LOG.info("DeploymentAgent produced plan: planId={}, actions={}, risk={}",
                    plan.planId(), actions.size(), riskLevel);

            return plan;

        } catch (Exception e) {
            LOG.error("DeploymentAgent failed for alertId={}", alertId, e);
            return createDefaultPlan(alertId, serviceId);
        }
    }

    /**
     * Determines rollback actions based on the deployment regression.
     */
    private List<RemediationAction> determineRollbackActions(
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        String lowerDesc = anomalyDescription.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        if (severity == Severity.CRITICAL ||
            lowerDesc.contains("crash") ||
            lowerDesc.contains("error rate") ||
            lowerCause.contains("deployment caused")) {

            return List.of(
                    new RemediationAction(
                            ActionType.ROLLBACK_DEPLOYMENT,
                            serviceId,
                            List.of(new Parameter("previousVersion", "auto-detect")),
                            "Critical regression detected post-deployment; recommend immediate rollback"
                    )
            );
        }

        return List.of(
                new RemediationAction(
                        ActionType.ROLLBACK_DEPLOYMENT,
                        serviceId,
                        List.of(new Parameter("previousVersion", "auto-detect")),
                        "Deployment regression detected; rollback to previous stable version"
                ),
                new RemediationAction(
                        ActionType.NOTIFY_ONCALL,
                        "sre-team",
                        List.of(new Parameter("reason", "rollback initiated")),
                        "Notify on-call SRE of rollback action"
                )
        );
    }

    /**
     * Determines risk level based on rollback actions and severity.
     */
    private RiskLevel determineRiskLevel(List<RemediationAction> actions, Severity severity) {
        if (severity == Severity.CRITICAL) {
            return RiskLevel.LOW;
        }
        if (severity == Severity.HIGH) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.HIGH;
    }

    /**
     * Creates a default plan when analysis fails.
     */
    private RemediationPlan createDefaultPlan(String alertId, String serviceId) {
        return new RemediationPlan(
                java.util.UUID.randomUUID(),
                getAgentName(),
                alertId,
                serviceId,
                List.of(new RemediationAction(
                        ActionType.ROLLBACK_DEPLOYMENT,
                        serviceId,
                        List.of(new Parameter("previousVersion", "auto-detect")),
                        "Deployment regression detected; rollback recommended"
                )),
                RiskLevel.MEDIUM,
                "Rollback deployment to resolve regression",
                Instant.now()
        );
    }
}