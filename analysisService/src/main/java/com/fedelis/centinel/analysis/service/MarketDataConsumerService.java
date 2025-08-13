package com.fedelis.centinel.analysis.service;

import com.fedelis.centinel.analysis.model.MarketDataEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataConsumerService {

    private final MarketDataPersistenceService persistenceService;
    private final MarketDataAnalysisService analysisService;

    @KafkaListener(
        topics = "coinbase-market-data",
        groupId = "${kafka.consumer.group-id:analysis-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMarketData(
        @Payload List<MarketDataEvent> marketDataEvents,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        @Header(KafkaHeaders.RECEIVED_PARTITION) List<Integer> partitions,
        @Header(KafkaHeaders.OFFSET) List<Long> offsets,
        Acknowledgment acknowledgment
    ) {
        try {
            log.info("✅ Received {} market data events from topic: {}", marketDataEvents.size(), topic);
            
            // Process events in parallel for better performance
            List<CompletableFuture<Boolean>> futures = marketDataEvents.parallelStream()
                .map(event -> CompletableFuture.supplyAsync(() -> {
                    try {
                        log.debug("🔄 Processing event: source={}}", 
                            event.getSource());
                        
                        // Persist to TimescaleDB
                        boolean persisted = persistenceService.persistMarketData(event);
                        
                        // Only trigger analysis if persistence was successful
                        if (persisted) {
                            analysisService.analyzeMarketData(event);
                        } else {
                            log.warn("⚠️ Skipping analysis due to persistence failure for event: source={}", 
                                event.getSource());
                        }
                        
                        return persisted;
                    } catch (Exception e) {
                        log.error("❌ Error processing event: source={}, error={}", 
                            event.getSource(), e.getMessage(), e);
                        return false;
                    }
                }))
                .collect(Collectors.toList());
            
            // Wait for all processing to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // Count successful and failed processing
            long successfulCount = futures.stream()
                .mapToLong(future -> {
                    try {
                        return future.get() ? 1 : 0;
                    } catch (Exception e) {
                        log.error("❌ Error getting future result: {}", e.getMessage());
                        return 0;
                    }
                })
                .sum();
            
            log.info("✅ Batch processing completed: {}/{} events processed successfully", 
                successfulCount, marketDataEvents.size());
            
            // Acknowledge the batch
            acknowledgment.acknowledge();
            log.debug("✅ Successfully processed and acknowledged batch of {} events", marketDataEvents.size());
            
        } catch (Exception e) {
            log.error("❌ Error processing market data batch: {}", e.getMessage(), e);
            // Don't acknowledge on error - let Kafka retry
            throw e;
        }
    }

}
