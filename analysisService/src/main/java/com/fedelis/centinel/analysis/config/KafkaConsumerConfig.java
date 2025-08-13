package com.fedelis.centinel.analysis.config;

import com.fedelis.centinel.analysis.model.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    /**
     * Kafka consumer configuration for the Analysis Service.
     * 
     * This configuration class sets up the Kafka consumer factory and listener container factory
     * for consuming market data events from Kafka topics. It configures JSON deserialization
     * for MarketDataEvent objects and provides error handling capabilities.
     * 
     * Key features:
     * - Configures consumer with manual acknowledgment mode for reliable message processing
     * - Sets up batch listening for improved throughput
     * - Uses ErrorHandlingDeserializer to gracefully handle deserialization failures
     * - Configures JSON deserialization without type headers for compatibility
     * - Supports concurrent message processing with configurable listener threads
     * 
     * @see MarketDataEvent
     * @see org.springframework.kafka.annotation.EnableKafka
     */

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:analysis-service-group}")
    private String groupId;

    @Value("${spring.kafka.topic.market-data:market-data}")
    private String marketDataTopic;

    @Value("${spring.kafka.consumer.listener-threads:3}")
    private int listenerThreads;

    @Bean
    public ConsumerFactory<String, MarketDataEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Configure for JSON without type headers
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new JsonDeserializer<>(MarketDataEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MarketDataEvent> kafkaListenerContainerFactory() {
        log.debug("🔄 initializing KafkaListenerContainerFactory");
        ConcurrentKafkaListenerContainerFactory<String, MarketDataEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        log.debug("🔄 setting KafkaListenerContainerFactory concurreny to {}", listenerThreads);
        factory.setConcurrency(listenerThreads); // Number of consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setBatchListener(true);
        
        // Error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (record, exception) -> {
                // Log the error and continue processing
                log.error("❌ Error processing record: {}, Exception: {}", record, exception.getMessage());
            }
        ));

        return factory;
    }
}
