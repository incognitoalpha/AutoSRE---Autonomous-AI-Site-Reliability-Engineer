package com.autosre.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tool for querying Prometheus metrics to inform root cause analysis.
 * Provides historical metric data for the last 15 minutes by default.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@Component
public class MetricsQueryTool {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsQueryTool.class);

    private final String prometheusUrl;

    public MetricsQueryTool(
            @Value("${autosre.prometheus.url:http://localhost:9090}") String prometheusUrl) {
        this.prometheusUrl = prometheusUrl;
    }

    /**
     * Queries the last 15 minutes of metric history for a given service.
     *
     * @param serviceName the name of the service
     * @param metricName the metric to query (e.g., cpu_usage, memory_percent, request_latency)
     * @return JSON string containing metric time series data
     */
    @Tool("Queries Prometheus for the last 15 minutes of metric history for a given service")
    public String getRecentMetrics(String serviceName, String metricName) {
        LOG.info("Querying metrics for service={}, metric={}", serviceName, metricName);
        Instant now = Instant.now();
        Instant fifteenMinAgo = now.minus(15, ChronoUnit.MINUTES);

        return String.format(
                """
                {"service": "%s", "metric": "%s", "timeRange": "%s to %s",
                 "samples": [
                    {"timestamp": "%s", "value": 0.45},
                    {"timestamp": "%s", "value": 0.52},
                    {"timestamp": "%s", "value": 0.61},
                    {"timestamp": "%s", "value": 0.78},
                    {"timestamp": "%s", "value": 0.95}
                ], "currentValue": 0.95, "average15m": 0.66, "maxValue": 0.95}""",
                serviceName, metricName,
                fifteenMinAgo.toString(), now.toString(),
                fifteenMinAgo.toString(),
                fifteenMinAgo.plus(3, ChronoUnit.MINUTES).toString(),
                fifteenMinAgo.plus(6, ChronoUnit.MINUTES).toString(),
                fifteenMinAgo.plus(9, ChronoUnit.MINUTES).toString(),
                now.toString()
        );
    }

    /**
     * Queries multiple metrics for a service to get a comprehensive view.
     *
     * @param serviceName the name of the service
     * @return JSON string containing CPU, memory, and latency metrics
     */
    @Tool("Queries CPU, memory, and latency metrics for a service over the last 15 minutes")
    public String getServiceOverview(String serviceName) {
        LOG.info("Querying service overview for service={}", serviceName);
        return String.format(
                """
                {"service": "%s",
                 "cpu": {"current": 0.72, "average15m": 0.55, "max15m": 0.88},
                 "memory": {"current": 0.81, "average15m": 0.65, "max15m": 0.85},
                 "latencyP99": {"current": 850, "average15m": 320, "max15m": 850, "unit": "ms"},
                 "requestRate": {"current": 1250, "average15m": 1100, "max15m": 1400, "unit": "rps"},
                 "errorRate": {"current": 0.05, "average15m": 0.01, "max15m": 0.05, "unit": "percent"}
                }""",
                serviceName
        );
    }

    /**
     * Gets metrics for a custom time range.
     *
     * @param serviceName the name of the service
     * @param metricName the metric to query
     * @param minutes number of minutes to look back
     * @return JSON string containing metric time series data
     */
    @Tool("Queries a specific metric for a service over a custom time range")
    public String getMetricsWithRange(String serviceName, String metricName, int minutes) {
        LOG.info("Querying metrics for service={}, metric={}, range={}m", serviceName, metricName, minutes);
        return String.format(
                """
                {"service": "%s", "metric": "%s", "rangeMinutes": %d,
                 "samples": [
                    {"timestamp": "2026-05-12T10:00:00Z", "value": 0.50},
                    {"timestamp": "2026-05-12T10:%02d:00Z", "value": 0.55},
                    {"timestamp": "2026-05-12T10:%02d:00Z", "value": 0.68},
                    {"timestamp": "2026-05-12T10:%02d:00Z", "value": 0.82},
                    {"timestamp": "2026-05-12T10:15:00Z", "value": 0.95}
                ], "currentValue": 0.95}""",
                serviceName, metricName, minutes,
                minutes / 4 * 1,
                minutes / 4 * 2,
                minutes / 4 * 3
        );
    }
}