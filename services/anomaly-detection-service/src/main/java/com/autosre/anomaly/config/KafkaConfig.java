package com.autosre.anomaly.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import com.autosre.anomaly.model.TelemetryEvent;

/**
 * Kafka configuration for anomaly detection service.
 * Sets up consumer factories with error handling and producer configuration.
 *
 * <p>Bounded context: {@code anomaly-detection-service}</p>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${autosre.kafka.consumer-groups.metrics-consumer}")
    private String metricsGroupId;

    @Value("${autosre.kafka.consumer-groups.logs-consumer}")
    private String logsGroupId;

    @Value("${autosre.kafka.error-handler.retries}")
    private int maxRetries;

    @Value("${autosre.kafka.error-handler.backoff-ms}")
    private long backOffMs;

    /**
     * Creates the metrics consumer factory with JSON deserialization.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, TelemetryEvent> metricsConsumerFactory() {
        Map<String, Object> props = baseConsumerProps(metricsGroupId);
        JsonDeserializer<TelemetryEvent> deserializer = new JsonDeserializer<>(TelemetryEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.autosre.anomaly.model");
        deserializer.setUseTypeMapperForKey(true);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Creates the logs consumer factory with JSON deserialization.
     *
     * @return the configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, TelemetryEvent> logsConsumerFactory() {
        Map<String, Object> props = baseConsumerProps(logsGroupId);
        JsonDeserializer<TelemetryEvent> deserializer = new JsonDeserializer<>(TelemetryEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.autosre.anomaly.model");
        deserializer.setUseTypeMapperForKey(true);
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    /**
     * Creates the Kafka listener container factory for metrics consumers.
     *
     * @return the configured listener container factory with error handling
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> metricsListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(metricsConsumerFactory());
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Creates the Kafka listener container factory for logs consumers.
     *
     * @return the configured listener container factory with error handling
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> logsListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TelemetryEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logsConsumerFactory());
        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    /**
     * Creates the producer factory for publishing anomaly alerts.
     *
     * @return the configured producer factory
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the Kafka template for sending messages.
     *
     * @return the configured Kafka template
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private Map<String, Object> baseConsumerProps(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private CommonErrorHandler errorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(backOffMs, maxRetries));
    }
}