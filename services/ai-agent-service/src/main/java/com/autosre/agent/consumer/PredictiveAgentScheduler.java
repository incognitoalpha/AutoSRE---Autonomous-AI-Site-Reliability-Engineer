package com.autosre.agent.consumer;

import com.autosre.agent.model.PredictiveAlert;
import com.autosre.agent.agent.PredictiveAgent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled consumer that runs PredictiveAgent on a 5-minute interval
 * against all monitored services stored in Redis. Evaluates metric
 * trends and emits PredictiveAlert events when failures are predicted.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 *
 * @see PredictiveAgent
 * @see PredictiveAlert
 */
@Component
public class PredictiveAgentScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PredictiveAgentScheduler.class);
    private static final String MONITORED_SERVICES_KEY = "autosre:monitored:services";
    private static final Duration METRICS_WINDOW = Duration.ofMinutes(30);
    private static final String METRICS_KEY_PREFIX = "autosre:metrics:";

    private final PredictiveAgent predictiveAgent;
    private final RedisTemplate<String, Object> redisTemplate;

    public PredictiveAgentScheduler(
            PredictiveAgent predictiveAgent,
            RedisTemplate<String, Object> redisTemplate) {
        this.predictiveAgent = predictiveAgent;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Runs PredictiveAgent analysis every 5 minutes against all monitored services.
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void runPredictiveAnalysis() {
        LOG.info("Starting predictive analysis cycle");

        try {
            List<String> monitoredServices = getMonitoredServices();

            if (monitoredServices.isEmpty()) {
                LOG.debug("No monitored services found, skipping predictive analysis");
                return;
            }

            LOG.info("Analyzing {} monitored services for predictive alerts", monitoredServices.size());

            List<PredictiveAlert> alerts = new ArrayList<>();

            for (String serviceId : monitoredServices) {
                try {
                    PredictiveAlert alert = analyzeService(serviceId);
                    if (alert != null) {
                        alerts.add(alert);
                        LOG.info("Predictive alert generated for service={}: type={}, timeToFailure={}min",
                                serviceId, alert.predictedFailureType(), alert.predictedTimeMinutes());
                    }
                } catch (Exception e) {
                    LOG.error("Failed to analyze service {}: {}", serviceId, e.getMessage());
                }
            }

            LOG.info("Predictive analysis cycle complete: {} alerts generated", alerts.size());

        } catch (Exception e) {
            LOG.error("Predictive analysis cycle failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieves the list of monitored services from Redis.
     */
    private List<String> getMonitoredServices() {
        try {
            var services = redisTemplate.opsForList().range(MONITORED_SERVICES_KEY, 0, -1);
            if (services == null || services.isEmpty()) {
                return List.of();
            }
            return services.stream()
                    .map(Object::toString)
                    .toList();
        } catch (Exception e) {
            LOG.warn("Failed to retrieve monitored services: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Analyzes a single service for predictive failures.
     */
    private PredictiveAlert analyzeService(String serviceId) {
        LOG.debug("Analyzing service: {}", serviceId);

        List<Double> memoryValues = retrieveMetricHistory(serviceId, "memory");
        List<Double> cpuValues = retrieveMetricHistory(serviceId, "cpu");
        List<Double> diskValues = retrieveMetricHistory(serviceId, "disk");

        var memoryActions = predictiveAgent.analyzeTrends(serviceId, memoryValues, "memory");
        var cpuActions = predictiveAgent.analyzeTrends(serviceId, cpuValues, "cpu");
        var diskActions = predictiveAgent.analyzeTrends(serviceId, diskValues, "disk");

        if (!memoryActions.isEmpty()) {
            return createAlert(serviceId, "memory_exhaustion", memoryValues,
                    memoryActions.stream().map(a -> a.reason()).toList());
        }

        if (!cpuActions.isEmpty()) {
            return createAlert(serviceId, "cpu_saturation", cpuValues,
                    cpuActions.stream().map(a -> a.reason()).toList());
        }

        if (!diskActions.isEmpty()) {
            return createAlert(serviceId, "disk_fill", diskValues,
                    diskActions.stream().map(a -> a.reason()).toList());
        }

        return null;
    }

    /**
     * Retrieves metric history from Redis.
     */
    @SuppressWarnings("unchecked")
    private List<Double> retrieveMetricHistory(String serviceId, String metricType) {
        try {
            String key = METRICS_KEY_PREFIX + serviceId + ":" + metricType;
            var values = redisTemplate.opsForList().range(key, -30, -1);
            if (values == null) {
                return List.of();
            }
            return values.stream()
                    .map(v -> {
                        if (v instanceof Double) {
                            return (Double) v;
                        }
                        if (v instanceof Number) {
                            return ((Number) v).doubleValue();
                        }
                        return Double.parseDouble(v.toString());
                    })
                    .toList();
        } catch (Exception e) {
            LOG.debug("Failed to retrieve {} metrics for {}: {}",
                    metricType, serviceId, e.getMessage());
            return List.of();
        }
    }

    /**
     * Creates a predictive alert from analysis results.
     */
    private PredictiveAlert createAlert(
            String serviceId,
            String failureType,
            List<Double> metricValues,
            List<String> recommendedActions) {

        if (metricValues.isEmpty()) {
            return null;
        }

        double currentValue = metricValues.get(metricValues.size() - 1);
        double projectedValue = extrapolateTrend(metricValues);
        double confidence = calculateConfidence(metricValues);

        int minutesToFailure = estimateTimeToFailure(metricValues, currentValue, projectedValue);

        return PredictiveAlert.create(
                serviceId,
                failureType,
                minutesToFailure,
                confidence,
                currentValue,
                projectedValue,
                List.of(serviceId),
                recommendedActions
        );
    }

    /**
     * Simple linear extrapolation of metric trend.
     */
    private double extrapolateTrend(List<Double> values) {
        if (values.size() < 3) {
            return values.isEmpty() ? 0.0 : values.get(values.size() - 1);
        }

        int n = values.size();
        double slope = 0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double denom = n * sumX2 - sumX * sumX;
        if (denom != 0) {
            slope = (n * sumXY - sumX * sumY) / denom;
        }

        double intercept = (sumY - slope * sumX) / n;
        return intercept + slope * (n + 4);
    }

    /**
     * Calculates confidence score based on trend stability.
     */
    private double calculateConfidence(List<Double> values) {
        if (values.size() < 5) {
            return 0.5;
        }

        double mean = values.stream().mapToDouble(v -> v).average().orElse(0.5);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.5);

        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / (mean > 0 ? mean : 1);

        double confidence = 1.0 - Math.min(coefficientOfVariation, 0.8);
        return Math.max(0.5, Math.min(0.95, confidence));
    }

    /**
     * Estimates minutes until failure threshold is reached.
     */
    private int estimateTimeToFailure(List<Double> values, double current, double projected) {
        if (values.size() < 3) {
            return 20;
        }

        int n = values.size();
        double slope = 0;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }

        double denom = n * sumX2 - sumX * sumX;
        if (denom != 0) {
            slope = (n * sumXY - sumX * sumY) / denom;
        }

        if (slope <= 0) {
            return 30;
        }

        double threshold = 0.95;
        return (int) Math.max(5, Math.min(30, (threshold - current) / slope * 5));
    }

    /**
     * Manually triggers predictive analysis for a specific service.
     * Can be called from REST endpoint or other services.
     */
    public PredictiveAlert triggerAnalysis(String serviceId) {
        LOG.info("Manual predictive analysis triggered for service={}", serviceId);
        return analyzeService(serviceId);
    }
}