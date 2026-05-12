package com.autosre.agent.agent;

import com.autosre.agent.rag.IncidentMemoryService;
import com.autosre.agent.rag.RunbookRetrievalService;
import com.autosre.agent.tool.KubernetesQueryTool;
import com.autosre.agent.tool.MetricsQueryTool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }
}