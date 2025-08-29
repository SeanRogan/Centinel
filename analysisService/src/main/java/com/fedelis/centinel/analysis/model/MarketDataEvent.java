package com.fedelis.centinel.analysis.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MarketDataEvent {
    /**
     * This class represents a Kafka event to be consumed by the MarketDataConsumerService.
     * It contains the message and the source of the event.
     * The message is the raw JSON string received from the Kafka topic.
     * The source is the name of the exchange or service that sent the event.
     */
    private String message;
    private String source;
    
    // Backward compatibility constructor
    public MarketDataEvent(String message, String source) {
        this.message = message;
        this.source = source;
    }
}

