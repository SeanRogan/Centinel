package com.fedelis.centinel.monitor.client;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Blueprint interface for WebSocket clients streaming market data from exchanges.
 * Implementations should handle connection lifecycle and message streaming.
 */
public interface ExchangeConnectorWebsocketClient {
    /**
     * Connects to the exchange WebSocket feed and starts streaming data.
     * @throws URISyntaxException if connection fails
     */
    void connect(List<String> symbols) throws URISyntaxException;

    /**
     * Disconnects from the exchange WebSocket feed.
     */
    void disconnect();

    /**
     * Checks if the client is currently connected to the WebSocket feed.
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Sends a custom message to the WebSocket server (e.g., for subscribing/unsubscribing channels).
     * @param message the message to send
     */
    void sendMessage(String message);
}