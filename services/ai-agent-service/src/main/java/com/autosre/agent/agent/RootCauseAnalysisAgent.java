package com.autosre.agent.agent;

import com.autosre.agent.model.RootCauseAnalysis;
import com.autosre.agent.rag.IncidentMemoryService;
import com.autosre.agent.rag.RunbookRetrievalService;
import com.autosre.agent.tool.KubernetesQueryTool;
import com.autosre.agent.tool.MetricsQueryTool;
import com.autosre.common.model.Severity;
import com.autosre.common.util.JsonUtils;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * LangChain4j-powered root cause analysis agent that uses RAG to retrieve
 * relevant runbooks and historical incidents, then performs structured analysis
 * to produce a {@link RootCauseAnalysis} with confidence scoring.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RootCauseAnalysis
 * @see BaseAgent
 */
@Component
public class RootCauseAnalysisAgent extends BaseAgent implements RootCauseAgent {

    private static final Logger LOG = LoggerFactory.getLogger(RootCauseAnalysisAgent.class);
    private static final String SYSTEM_PROMPT = """
            You are a Site Reliability Engineer (SRE) with deep expertise in distributed systems,
            Kubernetes, Apache Kafka, and incident root cause analysis. Your task is to analyze
            anomaly alerts and determine the root cause with high confidence.

            You have access to:
            - Real-time service metrics (CPU, memory, latency, error rate)
            - Kubernetes pod status and events
            - Relevant runbooks from our knowledge base
            - Historical incidents with similar patterns

            Output your analysis as a JSON object with the following structure:
            {
              "rootCause": "brief description of the root cause",
              "affectedComponents": ["component1", "component2"],
              "confidenceScore": 0.85,
              "recommendedActions": ["action1", "action2"],
              "evidenceSummary": "key evidence supporting this conclusion"
            }

            Guidelines:
            - confidenceScore must be between 0.0 and 1.0
            - Be conservative if evidence is ambiguous; prefer lower confidence
            - Consider similar historical incidents when determining root cause
            - Focus on actionable, specific root causes
            """;

    private final ChatLanguageModel chatModel;

    public RootCauseAnalysisAgent(
            ChatLanguageModel chatModel,
            IncidentMemoryService memoryService,
            RunbookRetrievalService runbookService,
            KubernetesQueryTool kubernetesTool,
            MetricsQueryTool metricsTool) {
        super(chatModel, memoryService, runbookService, kubernetesTool, metricsTool);
        this.chatModel = chatModel;
    }

    @Override
    protected String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    @Override
    public String getAgentName() {
        return "RootCauseAnalysisAgent";
    }

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
    @Override
    public RootCauseAnalysis analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription) {

        LOG.info("Starting RCA for alertId={}, service={}, severity={}",
                alertId, serviceId, severity);
        logExecutionEvent("STARTED", alertId, anomalyDescription);

        try {
            String context = buildContextPrompt(alertId, serviceId, severity, anomalyDescription);
            String userPrompt = buildUserPrompt(context, anomalyDescription);

            var response = chatModel.generate(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            ));

            String rawResponse = response.content() != null ? response.content().text() : "";

            RootCauseAnalysis rca = parseResponse(rawResponse, alertId, serviceId, severity);

            if (rca != null) {
                memoryService.store(rca);
                logExecutionEvent("COMPLETED", alertId,
                        String.format("rootCause=%s, confidence=%.2f",
                                rca.rootCause(), rca.confidenceScore()));
            }

            return rca != null ? rca : createDefaultRca(alertId, serviceId, severity, anomalyDescription);

        } catch (Exception e) {
            LOG.error("RCA failed for alertId={}", alertId, e);
            logExecutionEvent("FAILED", alertId, e.getMessage());
            return createDefaultRca(alertId, serviceId, severity, anomalyDescription);
        }
    }

    /**
     * Builds the user prompt with the gathered context.
     */
    private String buildUserPrompt(String context, String anomalyDescription) {
        return String.format("""
                Analyze the following incident and determine the root cause:

                INCIDENT:
                %s

                CONTEXT DATA:
                %s

                Based on the service metrics, pod status, events, relevant runbooks,
                and historical incidents provided above, identify the root cause
                and provide your analysis in JSON format.
                """, anomalyDescription, context);
    }

    /**
     * Parses the LLM JSON response into a RootCauseAnalysis record.
     */
    private RootCauseAnalysis parseResponse(
            String rawResponse,
            String alertId,
            String serviceId,
            Severity severity) {

        try {
            LOG.debug("Parsing RCA response: {}", rawResponse);

            RootCauseAnalysis parsed = JsonUtils.fromJson(rawResponse, RootCauseAnalysis.class);

            if (parsed != null && isValid(parsed)) {
                return parsed;
            }

            LOG.warn("Failed to parse valid RCA from response for alertId={}", alertId);
            return null;

        } catch (Exception e) {
            LOG.warn("JSON parse failed for RCA, attempting extraction: {}", e.getMessage());

            return extractFromText(rawResponse, alertId, serviceId, severity);
        }
    }

    /**
     * Validates that the parsed RCA has all required fields.
     */
    private boolean isValid(RootCauseAnalysis rca) {
        return rca.rootCause() != null && !rca.rootCause().isBlank()
                && rca.confidenceScore() >= 0.0 && rca.confidenceScore() <= 1.0;
    }

    /**
     * Fallback: extracts RCA fields from plain text response.
     */
    private RootCauseAnalysis extractFromText(
            String rawResponse,
            String alertId,
            String serviceId,
            Severity severity) {

        LOG.info("Using fallback extraction for alertId={}", alertId);

        String rootCause = extractField(rawResponse, "rootCause");
        List<String> components = extractListField(rawResponse, "affectedComponents");
        double confidence = extractConfidence(rawResponse);
        List<String> actions = extractListField(rawResponse, "recommendedActions");
        String evidence = extractField(rawResponse, "evidenceSummary");

        return new RootCauseAnalysis(
                rootCause != null ? rootCause : "Unable to determine root cause",
                components,
                confidence,
                actions,
                evidence != null ? evidence : rawResponse,
                alertId,
                serviceId,
                severity,
                Instant.now()
        );
    }

    private String extractField(String text, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> extractListField(String text, String fieldName) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\\[(.*?)\\]";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL).matcher(text);
        if (matcher.find()) {
            String content = matcher.group(1);
            java.util.regex.Pattern itemPattern = java.util.regex.Pattern.compile("\"([^\"]+)\"");
            java.util.regex.Matcher itemMatcher = itemPattern.matcher(content);
            List<String> items = new ArrayList<>();
            while (itemMatcher.find()) {
                items.add(itemMatcher.group(1));
            }
            return items;
        }
        return List.of();
    }

    private double extractConfidence(String text) {
        String pattern = "\"confidenceScore\"\\s*:\\s*([\\d.]+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0.5;
            }
        }
        return 0.5;
    }

    /**
     * Creates a default RCA when analysis fails.
     */
    private RootCauseAnalysis createDefaultRca(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription) {
        return new RootCauseAnalysis(
                "Root cause analysis inconclusive - manual review required",
                List.of(serviceId),
                0.3,
                List.of("Investigate manually", "Review recent deployments", "Check monitoring dashboards"),
                "RCA failed to produce valid analysis. Default response returned.",
                alertId,
                serviceId,
                severity,
                Instant.now()
        );
    }
}