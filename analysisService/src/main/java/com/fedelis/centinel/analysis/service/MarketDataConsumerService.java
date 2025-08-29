package com.fedelis.centinel.analysis.service;

import com.fedelis.centinel.analysis.model.MarketData;
import com.fedelis.centinel.analysis.model.MarketDataEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataConsumerService {

    private final MarketDataPersistenceService persistenceService;
    private final MultiTimeframeAnalysisService multiTimeframeAnalysisService;
    private final ObjectMapper objectMapper;

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
                        // Only add to multi-timeframe windows if persistence was successful
                        if (persisted) {
                            // Parse market data and add to multi-timeframe windows
                            try {
                                MarketData marketData = parseMarketDataFromEvent(event);
                                if (marketData != null) {
                                    multiTimeframeAnalysisService.addPriceData(
                                        marketData.getProductId(),
                                        marketData.getPrice(),
                                        marketData.getVolume24h(),
                                        marketData.getTime()
                                    );
                                    log.debug("✅ Added data to multi-timeframe windows for product: {}", 
                                        marketData.getProductId());
                                }
                            } catch (Exception e) {
                                log.error("❌ Error adding data to multi-timeframe windows: {}", e.getMessage());
                            }
                        } else {
                            log.warn("⚠️ Skipping multi-timeframe data addition due to persistence failure for event: source={}", 
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

    /**
     * Parses MarketData from MarketDataEvent message
     * @param event The market data event
     * @return MarketData object or null if parsing fails
     */
    private MarketData parseMarketDataFromEvent(MarketDataEvent event) {
        try {
            // Add null check for message
            if (event.getMessage() == null || event.getMessage().trim().isEmpty()) {
                log.error("❌ MarketDataEvent message is null or empty");
                return null;
            }
            
            String message = event.getMessage();
            JsonNode data;
            
            try {
                data = objectMapper.readTree(message);
            } catch (Exception e) {
                log.error("❌ Failed to parse JSON message: {}", message, e);
                return null;
            }
            
            MarketData marketData = new MarketData();
            
            // Set basic fields using JsonNode methods
            marketData.setType(getStringValue(data, "type"));
            marketData.setSequence(getIntegerValue(data, "sequence"));
            marketData.setProductId(getStringValue(data, "product_id"));
            marketData.setSide(getStringValue(data, "side"));
            marketData.setTradeId(getIntegerValue(data, "trade_id"));
            
            // Set numeric fields with proper conversion
            marketData.setPrice(getBigDecimalValue(data, "price"));
            marketData.setOpen24h(getBigDecimalValue(data, "open_24h"));
            marketData.setVolume24h(getBigDecimalValue(data, "volume_24h"));
            marketData.setLow24h(getBigDecimalValue(data, "low_24h"));
            marketData.setHigh24h(getBigDecimalValue(data, "high_24h"));
            marketData.setVolume30d(getBigDecimalValue(data, "volume_30d"));
            marketData.setBestBid(getBigDecimalValue(data, "best_bid"));
            marketData.setBestAsk(getBigDecimalValue(data, "best_ask"));
            marketData.setBestBidSize(getBigDecimalValue(data, "best_bid_size"));
            marketData.setBestAskSize(getBigDecimalValue(data, "best_ask_size"));
            marketData.setLastSize(getBigDecimalValue(data, "last_size"));
            
            // Handle time parsing with error handling
            String timeStr = getStringValue(data, "time");
            if (timeStr != null) {
                try {
                    marketData.setTime(Instant.parse(timeStr));
                } catch (Exception e) {
                    log.warn("⚠️ Could not parse time from data: {}, using current time", timeStr);
                    marketData.setTime(Instant.now());
                }
            } else {
                marketData.setTime(Instant.now());
            }
            
            marketData.setSource(event.getSource());
            marketData.setCreatedAt(Instant.now());
            
            log.debug("✅ Successfully parsed market data for product: {}", marketData.getProductId());
            return marketData;
                
        } catch (Exception e) {
            log.error("❌ Error parsing market data from event: {}", e.getMessage(), e);
            return null;
        }
    }
    
    // JsonNode helper methods
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }

    private Integer getIntegerValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                if (fieldNode.isInt()) {
                    return fieldNode.asInt();
                } else if (fieldNode.isLong()) {
                    return fieldNode.asInt();
                } else if (fieldNode.isTextual()) {
                    return Integer.parseInt(fieldNode.asText());
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Could not parse Integer from field {}: {}", fieldName, fieldNode.asText());
            }
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                if (fieldNode.isTextual()) {
                    return new BigDecimal(fieldNode.asText());
                } else if (fieldNode.isNumber()) {
                    return new BigDecimal(fieldNode.asText());
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ Could not parse BigDecimal from field {}: {}", fieldName, fieldNode.asText());
            }
        }
        return null;
    }

}
