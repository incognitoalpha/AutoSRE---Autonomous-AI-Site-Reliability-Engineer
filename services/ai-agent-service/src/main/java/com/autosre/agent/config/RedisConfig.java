package com.autosre.agent.config;

import com.autosre.agent.model.AgentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures Redis connectivity for hot-context storage and async approval gates.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Creates the Lettuce connection factory for Redis.
     *
     * @return configured {@link LettuceConnectionFactory}
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates the Redis template for storing {@link AgentContext} objects.
     * Serializes contexts as JSON for structured, queryable hot-context storage.
     *
     * @param connectionFactory the Redis connection factory
     * @return configured {@link RedisTemplate} for AgentContext storage
     */
    @Bean
    public RedisTemplate<String, AgentContext> agentContextRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AgentContext> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(agentContextObjectMapper(), AgentContext.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(agentContextObjectMapper(), AgentContext.class));
        return template;
    }

    private ObjectMapper agentContextObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}