package com.fedelis.centinel.analysis.service;

import com.fedelis.centinel.analysis.model.MarketData;
import com.fedelis.centinel.analysis.model.MarketDataEvent;
import com.fedelis.centinel.analysis.model.TradeSignal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataAnalysisService {

    private final TradeSignalGenerationService tradeSignalService;
    private final TradeSignalPersistenceService tradeSignalPersistenceService;
    private final ObjectMapper objectMapper;

    @Async
    public CompletableFuture<Void> analyzeMarketDataAsync(MarketDataEvent marketDataEvent) {
        try {
            log.debug("🔄 Starting async analysis for market data event: source={}", 
                marketDataEvent.getSource());
            
            performAnalysis(marketDataEvent);
            
            log.info("✅ Completed async analysis for market data event");
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("❌ Error in async analysis: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private void performAnalysis(MarketDataEvent marketDataEvent) {
        try {
            log.debug("Performing analysis on market data event: {}", marketDataEvent.getMessage());
            
            // Parse market data from the event message
            MarketData marketData = parseMarketDataFromEvent(marketDataEvent);
            if (marketData == null) {
                log.warn("⚠️ Could not parse market data from event: {}", marketDataEvent.getMessage());
                return;
            }
            
            // Generate trade signal asynchronously
            CompletableFuture<TradeSignal> signalFuture = tradeSignalService.generateTradeSignal(marketData);
            signalFuture.thenAccept(signal -> {
                if (signal != null) {
                    log.info("🎯 Trade signal generated: {} {} with confidence: {:.2f}", 
                        signal.getSignalType(), signal.getProductId(), signal.getConfidence());
                    
                    // TODO: Send signal to notification service
                    // TODO: Publish signal to Kafka topic for execution service
                    // TODO: Cache signal with short retention policy
                    
                } else {
                    log.debug("No trade signal generated for product: {}", marketData.getProductId());
                }
            }).exceptionally(throwable -> {
                log.error("❌ Error processing trade signal: {}", throwable.getMessage(), throwable);
                return null;
            });

        } catch (Exception e) {
            log.error("❌ Error performing analysis: {}", e.getMessage(), e);
            throw new IllegalStateException("❌ Analysis failed", e);
        }
    }
    

    //TODO: the methods below this are utility methods that are repeated elsewhere in the code. we should move them to a utility class at some point.
    /**
     * Parses MarketData from MarketDataEvent message using the same logic as MarketDataPersistenceService
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
    
    // JsonNode helper methods (same as in MarketDataPersistenceService)
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
