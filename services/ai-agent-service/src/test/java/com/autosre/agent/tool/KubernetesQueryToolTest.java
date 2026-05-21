package com.autosre.agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("KubernetesQueryTool")
class KubernetesQueryToolTest {

    private KubernetesQueryTool tool;

    @BeforeEach
    void setUp() {
        tool = new KubernetesQueryTool("http://localhost:8080", "default");
    }

    @Test
    @DisplayName("getPodStatus returns properly formatted JSON")
    void getPodStatusReturnsValidJson() {
        String status = tool.getPodStatus("my-service");
        assertNotNull(status);
        assertTrue(status.contains("\"service\": \"my-service\""));
        assertTrue(status.contains("\"namespace\": \"default\""));
        assertTrue(status.contains("\"my-service-pod-1\""));
    }

    @Test
    @DisplayName("getRecentEvents returns properly formatted JSON")
    void getRecentEventsReturnsValidJson() {
        String events = tool.getRecentEvents("my-service", 15);
        assertNotNull(events);
        assertTrue(events.contains("\"service\": \"my-service\""));
        assertTrue(events.contains("\"events\": ["));
        assertTrue(events.contains("BackOff"));
    }

    @Test
    @DisplayName("getDeploymentConfig returns properly formatted JSON")
    void getDeploymentConfigReturnsValidJson() {
        String config = tool.getDeploymentConfig("my-service");
        assertNotNull(config);
        assertTrue(config.contains("\"service\": \"my-service\""));
        assertTrue(config.contains("\"namespace\": \"default\""));
        assertTrue(config.contains("\"my-service:v1.2.3\""));
        assertTrue(config.contains("\"RollingUpdate\""));
    }

    @Test
    @DisplayName("handles null serviceName gracefully by formatting 'null'")
    void handlesNullServiceName() {
        String status = tool.getPodStatus(null);
        assertTrue(status.contains("\"service\": \"null\""));
        assertTrue(status.contains("\"null-pod-1\""));
    }
}
