package com.autosre.agent.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures LangChain4j chat and embedding models using the Anthropic API.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@Configuration
public class LangChainConfig {

    @Value("${langchain4j.anthropic.api-key}")
    private String apiKey;

    @Value("${langchain4j.anthropic.model}")
    private String modelName;

    @Value("${langchain4j.anthropic.max-tokens}")
    private int maxTokens;

    @Value("${langchain4j.anthropic.temperature}")
    private double temperature;

    /**
     * Creates the Anthropic chat model for LLM-based agent reasoning.
     *
     * @return configured {@link AnthropicChatModel} instance
     */
    @Bean
    public AnthropicChatModel anthropicChatModel() {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    /**
     * Creates the embedding model for generating text embeddings.
     * Uses the same Anthropic API credentials but via the OpenAI-compatible embedding interface
     * available in langchain4j-anthropic module.
     *
     * @return configured embedding model for RAG ingestion
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        // langchain4j-anthropic module provides embeddings via the same API key
        // using the OpenAI-compatible endpoint pattern
        return dev.langchain4j.model.openai.OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("claude-embedding-3-haiku-20250514")
                .build();
    }
}