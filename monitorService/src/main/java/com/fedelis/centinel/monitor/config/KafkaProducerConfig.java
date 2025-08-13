package com.fedelis.centinel.monitor.config;

import com.fedelis.centinel.monitor.model.MarketDataEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Bean
    public ProducerFactory<String, MarketDataEvent> producerFactory() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        configMap.put(JsonSerializer.TYPE_MAPPINGS, "market-data:com.fedelis.centinel.monitor.model.MarketDataEvent");
        configMap.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        
        // Optimize for low-latency financial data
        configMap.put(ProducerConfig.ACKS_CONFIG, "1"); // Fast acknowledgment
        configMap.put(ProducerConfig.RETRIES_CONFIG, 3);
        configMap.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configMap.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Low latency
        configMap.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new DefaultKafkaProducerFactory<>(configMap);
    }

    @Bean
    public KafkaTemplate<String, MarketDataEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

}
