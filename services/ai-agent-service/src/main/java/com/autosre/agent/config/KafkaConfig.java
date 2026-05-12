package com.autosre.agent.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.autosre.common.model.AnomalyAlert;

/**
 * Kafka configuration for AI agent service.
 * Consumes AnomalyAlert events with JSON deserialization.
 *
 * <p>Bounded context: {@code ai-agent-service}</p>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${autosre.kafka.consumer-groups.agent-service}")
    private String agentGroupId;

    @Value("${autosre.kafka.error-handler.retries:3}")
    private int maxRetries;

    @Value("${autosre.kafka.error-handler.backoff-ms:1000}")
    private long backOffMs;

    /**
     * Custom JSON deserializer for AnomalyAlert.
     * Handles the composite nature of the record including the nested BaselineStats.
     *
     * @return configured JSON deserializer
     */
    @Bean
    public JsonDeserializer<AnomalyAlert> anomalyAlertDeserializer() {
        JsonDeserializer<AnomalyAlert> deserializer = new JsonDeserializer<>(AnomalyAlert.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages(
                "com.autosre.anomaly.model",
                "com.autosre.common.model"
        );
        deserializer.setUseTypeMapperForKey(true);
        return deserializer;
    }

    /**
     * Consumer factory for AnomalyAlert events.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, AnomalyAlert> anomalyAlertConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, agentGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                anomalyAlertDeserializer()
        );
    }

    /**
     * Kafka listener container factory for AnomalyAlert consumers.
     *
     * @return the configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AnomalyAlert> anomalyAlertListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AnomalyAlert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(anomalyAlertConsumerFactory());
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    private CommonErrorHandler errorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(backOffMs, maxRetries));
    }
}
