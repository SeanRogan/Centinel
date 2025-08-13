package com.fedelis.centinel.analysis.service;

import com.fedelis.centinel.analysis.model.MarketData;
import com.fedelis.centinel.analysis.model.MarketDataEvent;
import com.fedelis.centinel.analysis.repository.MarketDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPersistenceService {

    private final MarketDataRepository marketDataRepository;
    private final ObjectMapper objectMapper;

    public boolean persistMarketData(MarketDataEvent marketDataEvent) {
        try {
            log.debug("🔄 Persisting market data event: source={}", 
                marketDataEvent.getSource());
            
            // Map the data directly to MarketData entity
            MarketData marketData = mapDataToMarketData(marketDataEvent);
            
            marketDataRepository.save(marketData);
            
            log.debug("✅ Successfully persisted market data event with ID: {}", marketData.getId());
            return true;
            
        } catch (Exception e) {
            log.error("❌ Error persisting market data event: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private MarketData mapDataToMarketData(MarketDataEvent event) {
        // Add null check for message
        if (event.getMessage() == null || event.getMessage().trim().isEmpty()) {
            log.error("❌ MarketDataEvent message is null or empty");
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        String message = event.getMessage();
        JsonNode data;
        
        try {
            data = objectMapper.readTree(message);
        } catch (Exception e) {
            log.error("❌ Failed to parse JSON message: {}", message, e);
            throw new RuntimeException("Invalid JSON in message", e);
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

        return marketData;
    }
    
    // New JsonNode helper methods
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
