package com.autosre.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetricsQueryTool")
class MetricsQueryToolTest {

    private MetricsQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new MetricsQueryTool("http://localhost:9090");
    }

    @Test
    @DisplayName("getRecentMetrics returns properly formatted JSON")
    void getRecentMetricsReturnsValidJson() {
        String metrics = tool.getRecentMetrics("my-service", "cpu_usage");
        assertNotNull(metrics);
        assertTrue(metrics.contains("\"service\": \"my-service\""));
        assertTrue(metrics.contains("\"metric\": \"cpu_usage\""));
        assertTrue(metrics.contains("\"samples\": ["));
    }

    @Test
    @DisplayName("getServiceOverview returns properly formatted JSON")
    void getServiceOverviewReturnsValidJson() {
        String overview = tool.getServiceOverview("my-service");
        assertNotNull(overview);
        assertTrue(overview.contains("\"service\": \"my-service\""));
        assertTrue(overview.contains("\"cpu\": {"));
        assertTrue(overview.contains("\"memory\": {"));
        assertTrue(overview.contains("\"latencyP99\": {"));
    }

    @Test
    @DisplayName("getMetricsWithRange returns properly formatted JSON")
    void getMetricsWithRangeReturnsValidJson() {
        String rangeMetrics = tool.getMetricsWithRange("my-service", "memory_percent", 30);
        assertNotNull(rangeMetrics);
        assertTrue(rangeMetrics.contains("\"service\": \"my-service\""));
        assertTrue(rangeMetrics.contains("\"metric\": \"memory_percent\""));
        assertTrue(rangeMetrics.contains("\"rangeMinutes\": 30"));
    }

    @Test
    @DisplayName("handles null inputs gracefully by formatting 'null'")
    void handlesNullInputs() {
        String metrics = tool.getRecentMetrics(null, null);
        assertTrue(metrics.contains("\"service\": \"null\""));
        assertTrue(metrics.contains("\"metric\": \"null\""));
    }
}
