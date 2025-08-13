package com.fedelis.centinel.monitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
public class MarketDataEvent {
    private String message;
    private String source;
    
    // Backward compatibility constructor
    public MarketDataEvent(String message, String source) {
        this.message = message;
        this.source = source;
    }
}

