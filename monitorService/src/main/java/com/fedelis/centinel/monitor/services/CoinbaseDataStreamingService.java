package com.fedelis.centinel.monitor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fedelis.centinel.monitor.client.CoinbaseWebsocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class CoinbaseDataStreamingService {

    private final CoinbaseWebsocketClient coinbaseClient;

    @Value("${market.data.symbols:BTC-USD}")
    private String assetSymbolsString;

    /**
     * Starts the market data streaming process.
     * Connects to Coinbase WebSocket and begins processing messages.
     */
    @Async
    public CompletableFuture<Void> startStreaming() {
        try {
            List<String> assetSymbols = Arrays.asList(assetSymbolsString.split(","));
            log.info("Starting market data streaming for symbols: {}", assetSymbols);
            
            // Connect with the specified symbols
            coinbaseClient.connect(assetSymbols);

            log.info("Market data streaming started successfully");
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Failed to start market data streaming", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Stops the market data streaming process.
     */
    public void stopStreaming() {
        log.info("Stopping market data streaming");
        coinbaseClient.disconnect();
    }
}
