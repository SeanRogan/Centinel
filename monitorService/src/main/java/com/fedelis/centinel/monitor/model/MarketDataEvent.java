package com.fedelis.centinel.monitor.model;

import lombok.Data;
import lombok.NoArgsConstructor;

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

