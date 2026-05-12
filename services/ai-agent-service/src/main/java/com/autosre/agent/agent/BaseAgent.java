package com.autosre.agent.agent;

import com.autosre.agent.rag.IncidentMemoryService;
import com.autosre.agent.rag.RunbookRetrievalService;
import com.autosre.agent.tool.KubernetesQueryTool;
import com.autosre.agent.tool.MetricsQueryTool;
import com.autosre.common.model.Severity;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all specialist agents in the AutoSRE system.
 * Provides common template methods for building prompts, parsing responses,
 * and storing context. Individual agents implement the abstract methods to
 * customize their behavior for their specific domain.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RootCauseAgent
 */
public abstract class BaseAgent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final ChatLanguageModel chatModel;
    protected final IncidentMemoryService memoryService;
    protected final RunbookRetrievalService runbookService;
    protected final KubernetesQueryTool kubernetesTool;
    protected final MetricsQueryTool metricsTool;

    protected BaseAgent(
            ChatLanguageModel chatModel,
            IncidentMemoryService memoryService,
            RunbookRetrievalService runbookService,
            KubernetesQueryTool kubernetesTool,
            MetricsQueryTool metricsTool) {
        this.chatModel = chatModel;
        this.memoryService = memoryService;
        this.runbookService = runbookService;
        this.kubernetesTool = kubernetesTool;
        this.metricsTool = metricsTool;
    }

    /**
     * Builds the system prompt for this agent based on its role and capabilities.
     *
     * @return the system prompt string
     */
    protected abstract String buildSystemPrompt();

    /**
     * Returns the name/identifier of this agent.
     *
     * @return the agent name
     */
    public abstract String getAgentName();

    /**
     * Builds a comprehensive context prompt by gathering information from
     * multiple sources: runbooks, historical incidents, Kubernetes state, and metrics.
     *
     * @param alertId the anomaly alert identifier
     * @param serviceId the affected service
     * @param severity the severity level
     * @param anomalyDescription description of the observed anomaly
     * @return enriched context string for the LLM
     */
    protected String buildContextPrompt(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription) {

        StringBuilder context = new StringBuilder();

        context.append("# INCIDENT CONTEXT\n\n");
        context.append(String.format("Alert ID: %s\n", alertId));
        context.append(String.format("Service: %s\n", serviceId));
        context.append(String.format("Severity: %s\n", severity));
        context.append(String.format("Anomaly: %s\n\n", anomalyDescription));

        context.append("## SERVICE METRICS\n");
        String metrics = metricsTool.getServiceOverview(serviceId);
        context.append(metrics).append("\n\n");

        context.append("## POD STATUS\n");
        String podStatus = kubernetesTool.getPodStatus(serviceId);
        context.append(podStatus).append("\n\n");

        context.append("## RECENT EVENTS\n");
        String events = kubernetesTool.getRecentEvents(serviceId, 15);
        context.append(events).append("\n\n");

        context.append("## RELEVANT RUNBOOKS\n");
        var runbookChunks = runbookService.retrieveRelevantRunbooks(anomalyDescription, 3);
        if (runbookChunks.isEmpty()) {
            context.append("No relevant runbooks found.\n\n");
        } else {
            for (var chunk : runbookChunks) {
                context.append(chunk.toPromptFragment()).append("\n");
            }
        }

        context.append("## HISTORICAL INCIDENTS\n");
        var similarIncidents = memoryService.findSimilar(anomalyDescription, 3);
        if (similarIncidents.isEmpty()) {
            context.append("No similar historical incidents found.\n\n");
        } else {
            for (var incident : similarIncidents) {
                context.append(String.format(
                        "- Service: %s, Root Cause: %s, Similarity: %.2f\n",
                        incident.serviceId(), incident.rootCause(), incident.similarityScore()
                ));
            }
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Stores the agent's reasoning trace and final output for auditability.
     *
     * @param alertId the alert this reasoning relates to
     * @param reasoningSteps the reasoning steps taken
     * @param finalOutput the final output produced
     */
    protected void storeContext(
            String alertId,
            List<String> reasoningSteps,
            String finalOutput) {
        log.info("Storing context for alertId={}, steps={}", alertId, reasoningSteps.size());
        log.debug("Final output for alertId={}: {}", alertId, finalOutput);
    }

    /**
     * Parses the LLM response into a structured format for downstream processing.
     * Subclasses should override this to parse their specific response format.
     *
     * @param rawResponse the raw LLM response string
     * @return parsed response or null if parsing fails
     */
    protected String parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            log.warn("Received empty response from LLM");
            return null;
        }
        return rawResponse.trim();
    }

    /**
     * Logs an agent execution event for auditing purposes.
     *
     * @param eventType the type of event (STARTED, COMPLETED, FAILED)
     * @param alertId the alert identifier
     * @param details additional event details
     */
    protected void logExecutionEvent(String eventType, String alertId, String details) {
        log.info("Agent execution event: type={}, alertId={}, agent={}, details={}",
                eventType, alertId, getAgentName(), details);
    }
}