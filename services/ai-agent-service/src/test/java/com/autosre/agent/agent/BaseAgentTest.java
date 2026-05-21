package com.autosre.agent.agent;

import com.autosre.agent.rag.IncidentMemoryService;
import com.autosre.agent.rag.RunbookRetrievalService;
import com.autosre.agent.tool.KubernetesQueryTool;
import com.autosre.agent.tool.MetricsQueryTool;
import com.autosre.common.model.Severity;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseAgent")
class BaseAgentTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private IncidentMemoryService memoryService;

    @Mock
    private RunbookRetrievalService runbookService;

    @Mock
    private KubernetesQueryTool kubernetesTool;

    @Mock
    private MetricsQueryTool metricsTool;

    @Test
    @DisplayName("allows null chatModel (no validation in constructor)")
    void allowsNullChatModel() {
        // BaseAgent does not validate null in constructor
        TestAgent agent = new TestAgent(null, memoryService, runbookService, kubernetesTool, metricsTool);
        assertNotNull(agent);
    }

    @Test
    @DisplayName("can construct with all non-null dependencies")
    void canConstructWithAllDependencies() {
        TestAgent agent = new TestAgent(chatModel, memoryService, runbookService, kubernetesTool, metricsTool);
        assertNotNull(agent);
        assertTrue(agent.getAgentName().contains("Test"));
    }

    @Test
    @DisplayName("buildContextPrompt handles empty runbooks and historical incidents")
    void buildContextPromptHandlesEmptyRagResults() {
        when(metricsTool.getServiceOverview(anyString())).thenReturn("metrics");
        when(kubernetesTool.getPodStatus(anyString())).thenReturn("podStatus");
        when(kubernetesTool.getRecentEvents(anyString(), anyInt())).thenReturn("events");
        when(runbookService.retrieveRelevantRunbooks(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(memoryService.findSimilar(anyString(), anyInt())).thenReturn(Collections.emptyList());

        TestAgent agent = new TestAgent(chatModel, memoryService, runbookService, kubernetesTool, metricsTool);
        String prompt = agent.buildContextPrompt("alert1", "service1", Severity.HIGH, "desc1");

        assertTrue(prompt.contains("No relevant runbooks found."));
        assertTrue(prompt.contains("No similar historical incidents found."));
    }

    @Test
    @DisplayName("parseResponse returns null for null or blank response")
    void parseResponseReturnsNullForEmpty() {
        TestAgent agent = new TestAgent(chatModel, memoryService, runbookService, kubernetesTool, metricsTool);
        assertNull(agent.testParseResponse(null));
        assertNull(agent.testParseResponse(""));
        assertNull(agent.testParseResponse("   "));
    }

    private static class TestAgent extends BaseAgent {
        protected TestAgent(
                ChatLanguageModel chatModel,
                IncidentMemoryService memoryService,
                RunbookRetrievalService runbookService,
                KubernetesQueryTool kubernetesTool,
                MetricsQueryTool metricsTool) {
            super(chatModel, memoryService, runbookService, kubernetesTool, metricsTool);
        }

        @Override
        protected String buildSystemPrompt() {
            return "Test agent prompt";
        }

        @Override
        public String getAgentName() {
            return "TestAgent";
        }

        public String buildContextPrompt(String alertId, String serviceId, Severity severity, String anomalyDescription) {
            return super.buildContextPrompt(alertId, serviceId, severity, anomalyDescription);
        }
        
        public String testParseResponse(String rawResponse) {
            return super.parseResponse(rawResponse);
        }
    }
}