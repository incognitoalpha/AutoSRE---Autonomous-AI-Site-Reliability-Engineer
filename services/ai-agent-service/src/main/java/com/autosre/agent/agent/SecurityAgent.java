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
 * Specialist agent that analyzes security anomalies and produces
 * remediation plans for security incidents. Always routes to SyncApprovalGate
 * due to the sensitive nature of security operations.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Component
public class SecurityAgent implements SpecialistAgent {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityAgent.class);

    public SecurityAgent() {
    }

    
    @Override
    public String getAgentName() {
        return "SecurityAgent";
    }

    @Override
    public boolean isApplicable(String anomalyType, String rootCause) {
        String lowerType = anomalyType.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        return lowerType.contains("security") ||
               lowerType.contains("auth") ||
               lowerType.contains("access") ||
               lowerType.contains("credential") ||
               lowerType.contains("token") ||
               lowerType.contains("unauthorized") ||
               lowerType.contains("injection") ||
               lowerType.contains("suspicious") ||
               lowerCause.contains("authentication") ||
               lowerCause.contains("authorization") ||
               lowerCause.contains("credential") ||
               lowerCause.contains("security");
    }

    @Override
    public RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        LOG.info("SecurityAgent analyzing: alertId={}, service={}, severity={}",
                alertId, serviceId, severity);

        try {
            List<RemediationAction> actions = determineSecurityActions(
                    serviceId, severity, anomalyDescription, rootCause);

            RemediationPlan plan = new RemediationPlan(
                    java.util.UUID.randomUUID(),
                    getAgentName(),
                    alertId,
                    serviceId,
                    actions,
                    RiskLevel.HIGH,
                    "Security remediation requiring human approval",
                    Instant.now()
            );

            LOG.info("SecurityAgent produced plan: planId={}, actions={}, risk=HIGH",
                    plan.planId(), actions.size());

            return plan;

        } catch (Exception e) {
            LOG.error("SecurityAgent failed for alertId={}", alertId, e);
            return createDefaultPlan(alertId, serviceId);
        }
    }

    /**
     * Determines appropriate security actions based on the incident.
     */
    private List<RemediationAction> determineSecurityActions(
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        String lowerDesc = anomalyDescription.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        if (lowerDesc.contains("unauthorized") || lowerDesc.contains("injection")) {
            return List.of(
                    new RemediationAction(
                            ActionType.QUARANTINE_POD,
                            serviceId,
                            List.of(new Parameter("reason", "Potential security compromise detected")),
                            "Quarantine service pending security investigation"
                    )
            );
        }

        if (lowerDesc.contains("credential") || lowerDesc.contains("token") ||
            lowerCause.contains("exposed")) {
            return List.of(
                    new RemediationAction(
                            ActionType.ROTATE_SECRETS,
                            serviceId,
                            List.of(new Parameter("scope", "service-level")),
                            "Rotate service credentials that may be compromised"
                    ),
                    new RemediationAction(
                            ActionType.NOTIFY_ONCALL,
                            "security-team",
                            List.of(new Parameter("priority", "high")),
                            "Alert security team to investigate"
                    )
            );
        }

        return List.of(
                new RemediationAction(
                        ActionType.NOTIFY_ONCALL,
                        "security-team",
                        List.of(new Parameter("priority", severity.name().toLowerCase())),
                        "Security incident requires human review"
                )
        );
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
                        ActionType.NOTIFY_ONCALL,
                        "security-team",
                        List.of(new Parameter("priority", "high")),
                        "Security alert requires immediate human review"
                )),
                RiskLevel.HIGH,
                "Security incident requiring human approval",
                Instant.now()
        );
    }
}