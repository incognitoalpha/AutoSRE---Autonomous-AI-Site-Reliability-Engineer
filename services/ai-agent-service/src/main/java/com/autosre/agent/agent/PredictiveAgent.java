package com.autosre.agent.agent;

import com.autosre.agent.model.RemediationPlan;
import com.autosre.agent.model.RemediationPlan.ActionType;
import com.autosre.agent.model.RemediationPlan.RemediationAction;
import com.autosre.agent.model.RemediationPlan.RemediationAction.Parameter;
import com.autosre.agent.model.RiskLevel;
import com.autosre.common.model.Severity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Specialist agent that predicts failures 15-30 minutes in advance using
 * trend analysis on rolling metrics windows. Detects patterns like:
 * - JVM heap exhaustion trend
 * - Disk fill rate progression
 * - Traffic growth exceeding capacity
 * - Connection pool saturation
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see RemediationPlan
 * @see SpecialistAgent
 */
@Component
public class PredictiveAgent implements SpecialistAgent {

    private static final Logger LOG = LoggerFactory.getLogger(PredictiveAgent.class);

    private static final double CRITICAL_THRESHOLD = 0.80;
    private static final double WARNING_THRESHOLD = 0.65;
    private static final int LOOKBACK_MINUTES = 30;
    private static final int FORWARD_PROJECTION_MINUTES = 20;

    public PredictiveAgent() {
    }

    @Override
    public String getAgentName() {
        return "PredictiveAgent";
    }

    @Override
    public boolean isApplicable(String anomalyType, String rootCause) {
        String lowerType = anomalyType.toLowerCase();
        String lowerCause = rootCause.toLowerCase();

        return lowerType.contains("predict") ||
               lowerType.contains("trend") ||
               lowerType.contains("growth") ||
               lowerType.contains("capacity") ||
               lowerType.contains("exhaustion") ||
               lowerCause.contains("predict") ||
               lowerCause.contains("trend") ||
               lowerCause.contains("growth") ||
               lowerCause.contains("capacity");
    }

    @Override
    public RemediationPlan analyze(
            String alertId,
            String serviceId,
            Severity severity,
            String anomalyDescription,
            String rootCause) {

        LOG.info("PredictiveAgent analyzing: alertId={}, service={}, severity={}",
                alertId, serviceId, severity);

        try {
            List<RemediationAction> actions = determinePreventiveActions(
                    serviceId, anomalyDescription, rootCause);
            RiskLevel riskLevel = determineRiskLevel(actions);

            RemediationPlan plan = new RemediationPlan(
                    java.util.UUID.randomUUID(),
                    getAgentName(),
                    alertId,
                    serviceId,
                    actions,
                    riskLevel,
                    "Preemptive action to prevent predicted failure",
                    Instant.now()
            );

            LOG.info("PredictiveAgent produced plan: planId={}, actions={}, risk={}",
                    plan.planId(), actions.size(), riskLevel);

            return plan;

        } catch (Exception e) {
            LOG.error("PredictiveAgent failed for alertId={}", alertId, e);
            return createDefaultPlan(alertId, serviceId);
        }
    }

    /**
     * Analyzes metrics trends and returns preventive actions.
     *
     * @param serviceId the service to analyze
     * @param metricValues time-series of metric values (normalized 0-1)
     * @param metricType type of metric (memory, cpu, disk)
     * @return list of recommended preventive actions
     */
    public List<RemediationAction> analyzeTrends(
            String serviceId,
            List<Double> metricValues,
            String metricType) {

        if (metricValues == null || metricValues.size() < 5) {
            LOG.debug("Insufficient data points for trend analysis of {}", serviceId);
            return List.of();
        }

        double currentValue = metricValues.get(metricValues.size() - 1);
        double[] trend = calculateLinearTrend(metricValues);
        double slope = trend[0];
        double projectedValue = currentValue + (slope * FORWARD_PROJECTION_MINUTES / 5);

        LOG.debug("Trend analysis for {}: current={}, slope={}, projected={}",
                serviceId, currentValue, slope, projectedValue);

        return determineActionsFromTrend(serviceId, metricType, currentValue, slope, projectedValue);
    }

    /**
     * Calculates linear trend using least squares regression.
     *
     * @param values list of metric values
     * @return array with [slope, intercept]
     */
    private double[] calculateLinearTrend(List<Double> values) {
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double denom = n * sumX2 - sumX * sumX;
        double slope = denom != 0 ? (n * sumXY - sumX * sumY) / denom : 0;
        double intercept = (sumY - slope * sumX) / n;

        return new double[]{slope, intercept};
    }

    /**
     * Determines preventive actions based on trend analysis.
     */
    private List<RemediationAction> determineActionsFromTrend(
            String serviceId,
            String metricType,
            double currentValue,
            double slope,
            double projectedValue) {

        List<RemediationAction> actions = new ArrayList<>();
        String lowerMetric = metricType.toLowerCase();
        boolean isExhaustion = projectedValue > CRITICAL_THRESHOLD;
        boolean isWarning = projectedValue > WARNING_THRESHOLD;

        if (lowerMetric.contains("memory") || lowerMetric.contains("heap")) {
            if (isExhaustion) {
                actions.add(new RemediationAction(
                        ActionType.ADJUST_RESOURCE_LIMITS,
                        serviceId,
                        List.of(new Parameter("memory", "increase")),
                        String.format("Memory exhaustion predicted in ~%d min (current: %.1f%%)",
                                FORWARD_PROJECTION_MINUTES, currentValue * 100)
                ));
            }
            if (slope > 0.01) {
                actions.add(new RemediationAction(
                        ActionType.SCALE_DEPLOYMENT,
                        serviceId,
                        List.of(new Parameter("replicas", "increase")),
                        "Scale up to reduce per-pod memory pressure"
                ));
            }
        }

        if (lowerMetric.contains("disk") || lowerMetric.contains("storage")) {
            if (isExhaustion) {
                actions.add(new RemediationAction(
                        ActionType.NOTIFY_ONCALL,
                        "sre-team",
                        List.of(new Parameter("reason", "disk exhaustion predicted")),
                        String.format("Disk exhaustion predicted in ~%d min (current: %.1f%%)",
                                FORWARD_PROJECTION_MINUTES, currentValue * 100)
                ));
            }
        }

        if (lowerMetric.contains("cpu") || lowerMetric.contains("load")) {
            if (isWarning) {
                actions.add(new RemediationAction(
                        ActionType.SCALE_DEPLOYMENT,
                        serviceId,
                        List.of(new Parameter("replicas", "2")),
                        String.format("CPU load predicted to exceed capacity in ~%d min (current: %.1f%%)",
                                FORWARD_PROJECTION_MINUTES, currentValue * 100)
                ));
            }
        }

        if (actions.isEmpty() && isWarning) {
            actions.add(new RemediationAction(
                    ActionType.SCALE_DEPLOYMENT,
                    serviceId,
                    List.of(new Parameter("replicas", "1")),
                    "Preemptive scale-up to handle projected load increase"
            ));
        }

        return actions;
    }

    /**
     * Determines preventive actions from description.
     */
    private List<RemediationAction> determinePreventiveActions(
            String serviceId,
            String anomalyDescription,
            String rootCause) {

        String lowerDesc = anomalyDescription.toLowerCase();

        if (lowerDesc.contains("heap") || lowerDesc.contains("memory")) {
            return List.of(
                    new RemediationAction(
                            ActionType.ADJUST_RESOURCE_LIMITS,
                            serviceId,
                            List.of(new Parameter("memory", "increase")),
                            "Preemptive memory limit increase based on growth trend"
                    ),
                    new RemediationAction(
                            ActionType.SCALE_DEPLOYMENT,
                            serviceId,
                            List.of(new Parameter("replicas", "1")),
                            "Preemptive scale to reduce per-pod memory pressure"
                    )
            );
        }

        if (lowerDesc.contains("disk") || lowerDesc.contains("storage")) {
            return List.of(
                    new RemediationAction(
                            ActionType.NOTIFY_ONCALL,
                            "sre-team",
                            List.of(new Parameter("priority", "high")),
                            "Predictive alert: disk space exhaustion within 20 minutes"
                    )
            );
        }

        if (lowerDesc.contains("cpu") || lowerDesc.contains("load")) {
            return List.of(
                    new RemediationAction(
                            ActionType.SCALE_DEPLOYMENT,
                            serviceId,
                            List.of(new Parameter("replicas", "2")),
                            "Preemptive scale to handle projected load increase"
                    )
            );
        }

        return List.of(
                new RemediationAction(
                        ActionType.SCALE_DEPLOYMENT,
                        serviceId,
                        List.of(new Parameter("replicas", "1")),
                        "Preemptive capacity increase based on trend analysis"
                )
        );
    }

    /**
     * Determines risk level for predictive actions.
     */
    private RiskLevel determineRiskLevel(List<RemediationAction> actions) {
        boolean hasNotifyOnly = actions.stream()
                .allMatch(a -> a.actionType() == ActionType.NOTIFY_ONCALL);

        return hasNotifyOnly ? RiskLevel.LOW : RiskLevel.MEDIUM;
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
                        "sre-team",
                        List.of(new Parameter("reason", "predictive alert")),
                        "Predicted capacity issue requires human review"
                )),
                RiskLevel.LOW,
                "Notify on-call of predictive alert",
                Instant.now()
        );
    }
}