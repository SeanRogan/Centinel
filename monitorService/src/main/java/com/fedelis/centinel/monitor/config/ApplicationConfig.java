package com.fedelis.centinel.monitor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fedelis.centinel.monitor.services.CoinbaseDataStreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@Configuration
@EnableAsync
@EnableKafka
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfig {

    private final CoinbaseDataStreamingService coinbaseDataStreamingService;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    /**
     * Starts the market data streaming service when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startMarketDataStreaming() {
        log.info("Application is ready, starting market data streaming...");
        coinbaseDataStreamingService.startStreaming()
            .thenRun(() -> log.info("Market data streaming started successfully"))
            .exceptionally(throwable -> {
                log.error("Failed to start market data streaming", throwable);
                return null;
            });
    }
}
