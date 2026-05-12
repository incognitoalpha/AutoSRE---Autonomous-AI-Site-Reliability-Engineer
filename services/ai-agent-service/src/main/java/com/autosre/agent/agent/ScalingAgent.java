package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RemediationPlan.ActionType;
import com.autosre.agent.model.RemediationPlan.RemediationAction;
import com.autosre.agent.model.RemediationPlan.RemediationAction.Parameter;
import com.autosre.agent.model.RiskLevel;
import com.autosre.common.model.Severity;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Specialist agent that analyzes resource saturation signals and produces
 * remediation plans for scaling operations. Handles horizontal pod autoscaling,
 * Kafka broker scaling, and other capacity-related issues.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Component
public class ScalingAgent implements SpecialistAgent {

    private static final Logger LOG = LoggerFactory.getLogger(ScalingAgent.class);
    private static final String SYSTEM_PROMPT = """
            You are a Scaling Specialist Agent for an AI-powered SRE system.
            Your expertise is in capacity management, horizontal pod autoscaling (HPA),
            Kafka broker scaling, and resource allocation optimization.

            You analyze metrics patterns to determine:
            1. Whether scaling is needed (CPU > 70%, memory > 80%, latency > p95 threshold)
            2. What type of scaling action is appropriate (scale up, scale down, adjust HPA)
            3. The recommended number of replicas or resource allocation
            4. Whether this is a transient spike or sustained load requiring permanent scaling

            You produce remediation plans with:
            - SCALE_DEPLOYMENT: scale Kubernetes Deployment replicas
            - SCALE_BROKERS: scale Kafka broker count
            - ADJUST_RESOURCE_LIMITS: modify CPU/memory limits

            Risk assessment:
            - LOW: Small scale-up (1-2 replicas), within current capacity headroom
            - MEDIUM: Significant scale-up (>2 replicas) or resource limit changes
            - HIGH: Scale-down operations or actions that may impact availability
            """;

    private final ChatLanguageModel chatModel;

    public ScalingAgent(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String getAgentName() {
        return "ScalingAgent";
    }

    @Override
    public boolean isApplicable(String anomalyType, String rootCause) {
        String lowerType = anomalyType.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        return lowerType.contains("cpu") || lowerType.contains("memory") ||
               lowerType.contains("scale") || lowerType.contains("capacity") ||
               lowerType.contains("resource") || lowerType.contains("hpa") ||
               lowerType.contains("broker") || lowerType.contains("throughput") ||
               lowerCause.contains("resource saturation") ||
               lowerCause.contains("capacity") ||
               lowerCause.contains("scaling") ||
               lowerCause.contains("load");
    }

    @Override
    public RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        LOG.info("ScalingAgent analyzing: alertId={}, service={}, severity={}",
                alertId, serviceId, severity);

        try {
            List<RemediationAction> actions = determineScalingActions(serviceId, severity);
            RiskLevel riskLevel = determineRiskLevel(actions, severity);

            RemediationPlan plan = new RemediationPlan(
                    java.util.UUID.randomUUID(),
                    getAgentName(),
                    alertId,
                    serviceId,
                    actions,
                    riskLevel,
                    "Adjust capacity to handle current load",
                    Instant.now()
            );

            LOG.info("ScalingAgent produced plan: planId={}, actions={}, risk={}",
                    plan.planId(), actions.size(), riskLevel);

            return plan;

        } catch (Exception e) {
            LOG.error("ScalingAgent failed for alertId={}", alertId, e);
            return createDefaultPlan(alertId, serviceId);
        }
    }

    /**
     * Determines scaling actions using LLM analysis.
     */
    private List<RemediationAction> determineScalingActions(
            String serviceId,
            Severity severity) {

        try {
            var response = chatModel.generate(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage("Analyze this scaling scenario for service: " + serviceId +
                            "\n\nProvide a JSON response with:\n" +
                            "{\n  \"actionType\": \"SCALE_DEPLOYMENT\" | \"SCALE_BROKERS\" | \"ADJUST_RESOURCE_LIMITS\",\n" +
                            "  \"target\": \"service-name\",\n" +
                            "  \"replicas\": \"2\",\n" +
                            "  \"reason\": \"explanation\"\n" +
                            "}")
            ));

            String rawResponse = response.content() != null ? response.content().text() : "";
            return parseScalingResponse(rawResponse, serviceId);

        } catch (Exception e) {
            LOG.warn("LLM scaling analysis failed, using default: {}", e.getMessage());
            return createDefaultScalingActions(serviceId);
        }
    }

    /**
     * Parses the LLM response to extract scaling actions.
     */
    private List<RemediationAction> parseScalingResponse(String rawResponse, String serviceId) {
        try {
            if (rawResponse == null || rawResponse.isBlank()) {
                return createDefaultScalingActions(serviceId);
            }

            String actionType = extractJsonField(rawResponse, "actionType");
            String target = extractJsonField(rawResponse, "target", serviceId);
            String replicas = extractJsonField(rawResponse, "replicas", "2");

            if (actionType == null) {
                return createDefaultScalingActions(serviceId);
            }

            ActionType type = ActionType.valueOf(actionType);
            List<Parameter> params = type == ActionType.SCALE_DEPLOYMENT ?
                    List.of(new Parameter("replicas", replicas)) :
                    List.of(new Parameter("count", replicas));

            return List.of(new RemediationAction(
                    type,
                    target,
                    params,
                    "LLM-recommended scaling action based on resource analysis"
            ));

        } catch (Exception e) {
            LOG.warn("Failed to parse scaling response, using default: {}", e.getMessage());
            return createDefaultScalingActions(serviceId);
        }
    }

    /**
     * Creates default scaling actions for when LLM analysis fails.
     */
    private List<RemediationAction> createDefaultScalingActions(String serviceId) {
        return List.of(new RemediationAction(
                ActionType.SCALE_DEPLOYMENT,
                serviceId,
                List.of(new Parameter("replicas", "2")),
                "Default scale-up: adding 1 replica to handle elevated load"
        ));
    }

    /**
     * Determines risk level based on scaling actions and severity.
     */
    private RiskLevel determineRiskLevel(List<RemediationAction> actions, Severity severity) {
        return switch (severity) {
            case CRITICAL -> RiskLevel.LOW;
            case HIGH -> RiskLevel.MEDIUM;
            case MEDIUM -> RiskLevel.LOW;
            case LOW -> RiskLevel.LOW;
        };
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
                createDefaultScalingActions(serviceId),
                RiskLevel.LOW,
                "Scale up to handle increased load",
                Instant.now()
        );
    }

    /**
     * Extracts a JSON field value from a string response.
     */
    private String extractJsonField(String json, String fieldName) {
        return extractJsonField(json, fieldName, null);
    }

    private String extractJsonField(String json, String fieldName, String defaultValue) {
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"?([^\",\\n}]+)";
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(pattern).matcher(json);
        return matcher.find() ? matcher.group(1).trim() : defaultValue;
    }
}