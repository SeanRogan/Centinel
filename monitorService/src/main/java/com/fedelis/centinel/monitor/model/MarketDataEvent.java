package com.fedelis.centinel.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataEvent {
    private String message;
    private String source;
    private Instant timestamp;

    public MarketDataEvent(String message, String source) {
        this.message = message;
        this.source = source;
        this.timestamp = Instant.now();
    }
}

