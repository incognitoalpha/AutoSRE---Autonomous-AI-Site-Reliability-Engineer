package com.autosre.agent.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.autosre.agent.model.AgentContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RedisConfig}.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
class RedisConfigTest {

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
    }

    @Test
    @DisplayName("Should create LettuceConnectionFactory with correct host and port")
    void redisConnectionFactoryCreatesCorrectly() {
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        assertThat(factory).isNotNull();
    }

    @Test
    @DisplayName("Should create RedisTemplate with correct serializers for AgentContext")
    void agentContextRedisTemplateHasCorrectSerializers() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, AgentContext> template =
                redisConfig.agentContextRedisTemplate(mock(RedisConnectionFactory.class));

        assertThat(template).isNotNull();
        assertThat(template.getValueSerializer()).isInstanceOf(Jackson2JsonRedisSerializer.class);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
    }

    @Test
    @DisplayName("Should register JavaTimeModule for Instant serialization")
    void agentContextRedisTemplateUsesJavaTimeModule() {
        ObjectMapper mapper = ReflectionTestUtils.invokeMethod(redisConfig, "agentContextObjectMapper");

        assertThat(mapper).isNotNull();
        assertThat(mapper.findModules().stream()
                .anyMatch(m -> m.getClass().getName().contains("JavaTimeModule"))).isTrue();
    }

    private RedisConnectionFactory mock(Class<RedisConnectionFactory> class1) {
        return org.mockito.Mockito.mock(class1);
    }
}