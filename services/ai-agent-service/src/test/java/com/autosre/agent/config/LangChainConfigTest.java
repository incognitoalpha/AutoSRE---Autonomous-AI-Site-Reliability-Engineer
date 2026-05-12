package com.autosre.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link LangChainConfig}.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
class LangChainConfigTest {

    private LangChainConfig langChainConfig;

    @BeforeEach
    void setUp() {
        langChainConfig = new LangChainConfig();
        ReflectionTestUtils.setField(langChainConfig, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(langChainConfig, "modelName", "claude-sonnet-4-20250514");
        ReflectionTestUtils.setField(langChainConfig, "maxTokens", 4096);
        ReflectionTestUtils.setField(langChainConfig, "temperature", 0.1);
    }

    @Test
    @DisplayName("Should create AnthropicChatModel with correct configuration")
    void anthropicChatModelCreatesCorrectly() {
        AnthropicChatModel model = langChainConfig.anthropicChatModel();

        assertThat(model).isNotNull();
        assertThat(model).isInstanceOf(AnthropicChatModel.class);
    }

    @Test
    @DisplayName("Should create EmbeddingModel for RAG operations")
    void embeddingModelCreatesCorrectly() {
        EmbeddingModel model = langChainConfig.embeddingModel();

        assertThat(model).isNotNull();
        assertThat(model).isInstanceOf(EmbeddingModel.class);
    }
}