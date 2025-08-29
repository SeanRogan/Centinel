package com.fedelis.centinel.analysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableScheduling
@EnableTransactionManagement
public class ApplicationConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // Configure ObjectMapper to handle Java 8 date/time types (like Instant)
        // and serialize dates as ISO-8601 strings instead of timestamps
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

}
