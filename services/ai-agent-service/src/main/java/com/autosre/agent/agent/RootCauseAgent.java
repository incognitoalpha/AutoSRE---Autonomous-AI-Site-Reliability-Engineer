package com.autosre.agent.agent;

import com.autosre.agent.model.RootCauseAnalysis;
import com.autosre.common.model.Severity;

/**
 * LangChain4j agent for performing root cause analysis on anomaly alerts.
 * Uses retrieved runbooks and historical incidents from pgvector to generate
 * structured root cause analysis with confidence scores.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RootCauseAnalysis
 * @see com.autosre.agent.rag.RunbookRetrievalService
 * @see com.autosre.agent.rag.IncidentMemoryService
 */
public interface RootCauseAgent {

    /**
     * Performs structured root cause analysis for the given anomaly alert.
     * Enriches context from vector DB and returns a {@link RootCauseAnalysis}.
     *
     * @param alertId unique identifier of the anomaly alert
     * @param serviceId the affected service identifier
     * @param severity the severity of the anomaly
     * @param anomalyDescription plain-language description of the observed anomaly
     * @return structured root cause analysis with confidence score
     */
    RootCauseAnalysis analyze(String alertId, String serviceId, Severity severity, String anomalyDescription);
}