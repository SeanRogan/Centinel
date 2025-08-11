package com.fedelis.centinel.monitor.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ExchangeDataStreamingService {
    CompletableFuture<Void> startStreaming(List<String> symbols);

    CompletableFuture<Void> stopStreaming();

    String getExchangeName();
}
