package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.common.model.Severity;

/**
 * Interface for specialist agents that analyze specific types of anomalies
 * and produce remediation plans tailored to their domain expertise.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 */
public interface SpecialistAgent {

    /**
     * Analyzes the given anomaly context and produces a remediation plan.
     *
     * @param alertId the anomaly alert identifier
     * @param serviceId the affected service identifier
     * @param severity the severity level of the anomaly
     * @param anomalyDescription plain-language description of the anomaly
     * @param rootCause the identified root cause from the RootCauseAgent
     * @return a remediation plan specific to this agent's domain, or null if not applicable
     */
    RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause
    );

    /**
     * Returns the name/identifier of this specialist agent.
     *
     * @return the agent name
     */
    String getAgentName();

    /**
     * Checks if this agent is applicable to the given anomaly type.
     *
     * @param anomalyType the type of anomaly
     * @param rootCause the identified root cause
     * @return true if this agent should process this anomaly
     */
    boolean isApplicable(String anomalyType, String rootCause);
}