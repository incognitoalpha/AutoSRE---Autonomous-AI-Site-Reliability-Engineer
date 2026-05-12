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
 * Specialist agent that handles pod-level recovery issues such as
 * CrashLoopBackOff, OOM kills, and restarts. Produces plans for pod
 * restarts with resource limit adjustments.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Component
public class RecoveryAgent implements SpecialistAgent {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryAgent.class);

    public RecoveryAgent() {
    }

    @Override
    public String getAgentName() {
        return "RecoveryAgent";
    }

    @Override
    public boolean isApplicable(String anomalyType, String rootCause) {
        String lowerType = anomalyType.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        return lowerType.contains("crash") ||
               lowerType.contains("restart") ||
               lowerType.contains("oom") ||
               lowerType.contains("kill") ||
               lowerType.contains("evict") ||
               lowerType.contains("image") ||
               lowerType.contains("fail") ||
               lowerType.contains("unhealthy") ||
               lowerCause.contains("crashloop") ||
               lowerCause.contains("oom") ||
               lowerCause.contains("memory") ||
               lowerCause.contains("restart") ||
               lowerCause.contains("evicted");
    }

    @Override
    public RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        LOG.info("RecoveryAgent analyzing: alertId={}, service={}, severity={}",
                alertId, serviceId, severity);

        try {
            List<RemediationAction> actions = determineRecoveryActions(
                    serviceId, severity, anomalyDescription, rootCause);
            RiskLevel riskLevel = determineRiskLevel(actions, severity);

            RemediationPlan plan = new RemediationPlan(
                    java.util.UUID.randomUUID(),
                    getAgentName(),
                    alertId,
                    serviceId,
                    actions,
                    riskLevel,
                    "Pod recovery with resource adjustment",
                    Instant.now()
            );

            LOG.info("RecoveryAgent produced plan: planId={}, actions={}, risk={}",
                    plan.planId(), actions.size(), riskLevel);

            return plan;

        } catch (Exception e) {
            LOG.error("RecoveryAgent failed for alertId={}", alertId, e);
            return createDefaultPlan(alertId, serviceId);
        }
    }

    /**
     * Determines recovery actions based on the pod failure type.
     */
    private List<RemediationAction> determineRecoveryActions(
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        String lowerDesc = anomalyDescription.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        if (lowerDesc.contains("oom") || lowerDesc.contains("memory") ||
            lowerCause.contains("oom") || lowerCause.contains("memory")) {
            return List.of(
                    new RemediationAction(
                            ActionType.RESTART_POD,
                            serviceId,
                            List.of(new Parameter("strategy", "rolling")),
                            "Restart pods to recover from OOM"
                    ),
                    new RemediationAction(
                            ActionType.ADJUST_RESOURCE_LIMITS,
                            serviceId,
                            List.of(
                                    new Parameter("memory", "increase"),
                                    new Parameter("increaseAmount", "50%")
                            ),
                            "Increase memory limits to prevent future OOM"
                    )
            );
        }

        if (lowerDesc.contains("crash") || lowerDesc.contains("restart loop")) {
            return List.of(
                    new RemediationAction(
                            ActionType.RESTART_POD,
                            serviceId,
                            List.of(
                                    new Parameter("strategy", "immediate"),
                                    new Parameter("force", "true")
                            ),
                            "Force restart pods to exit CrashLoopBackOff"
                    ),
                    new RemediationAction(
                            ActionType.NOTIFY_ONCALL,
                            "sre-team",
                            List.of(new Parameter("reason", "crash loop detected")),
                            "Notify SRE to investigate root cause of crash loop"
                    )
            );
        }

        return List.of(
                new RemediationAction(
                        ActionType.RESTART_POD,
                        serviceId,
                        List.of(new Parameter("strategy", "graceful")),
                        "Restart unhealthy pods to restore service"
                )
        );
    }

    /**
     * Determines risk level based on recovery actions.
     */
    private RiskLevel determineRiskLevel(List<RemediationAction> actions, Severity severity) {
        boolean hasResourceAdjustment = actions.stream()
                .anyMatch(a -> a.actionType() == ActionType.ADJUST_RESOURCE_LIMITS);

        if (severity == Severity.CRITICAL || hasResourceAdjustment) {
            return RiskLevel.LOW;
        }
        if (severity == Severity.HIGH) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
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
                        ActionType.RESTART_POD,
                        serviceId,
                        List.of(new Parameter("strategy", "graceful")),
                        "Restart pods to recover service"
                )),
                RiskLevel.LOW,
                "Pod restart to recover from failure",
                Instant.now()
        );
    }
}