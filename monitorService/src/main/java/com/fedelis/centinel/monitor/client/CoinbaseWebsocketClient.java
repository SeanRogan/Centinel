package com.fedelis.centinel.monitor.client;

import com.fedelis.centinel.monitor.model.MarketDataEvent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.java_websocket.client.WebSocketClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Data
@Service
@Slf4j
public class CoinbaseWebsocketClient implements ExchangeConnectorWebsocketClient{
    /**
     * Connects to the Coinbase WebSocket feed and starts streaming data.
     * Authenticates if API credentials are provided.
     */
    @Value("${coinbase.api.key:}")
    private String apiKey;
    @Value("${coinbase.api.secret:}")
    private String apiSecret;
    @Value("${coinbase.api.passphrase:}")
    private String passphrase;
    @Value("${coinbase.api.url.public}")
    private String PUBLIC_COINBASE_WS_URL;
    @Value("${coinbase.api.url.private}")
    private String PRIVATE_COINBASE_WS_URL;
    @Value("${kafka.topic.market-data:coinbase-market-data}")
    private String kafkaTopic;
    private final String exchangeName = "coinbase";
    private boolean connected = false;
    private boolean authenticated = false;
    private List<String> subscribedSymbols = new ArrayList<>();
    private KafkaTemplate<String, MarketDataEvent> kafkaTemplate;
    private WebSocketClient webSocketClient;

    @Autowired
    public CoinbaseWebsocketClient(KafkaTemplate<String, MarketDataEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Connects to the Coinbase WebSocket feed with specific symbols.
     * @param symbols list of symbols to subscribe to
     */
    @Override
    public void connect(List<String> symbols) throws URISyntaxException {
        if (connected) return;
        this.subscribedSymbols = symbols != null ? symbols : new ArrayList<>();

        // Use public URL for market data streaming
        String wsUrl = PUBLIC_COINBASE_WS_URL;
        log.info("Connecting to Coinbase WebSocket at: {}", wsUrl);

        webSocketClient = new WebSocketClient(new URI(wsUrl)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                try {
                    // Subscribe to ticker channel for the specified symbols
                    String subscribeMessage = buildSubscribeMessage(subscribedSymbols);
                    webSocketClient.send(subscribeMessage);
                    connected = true;
                    log.info("Connected to Coinbase WebSocket and subscribed to symbols: {}", subscribedSymbols);
                } catch (Exception e) {
                    connected = false;
                    log.error("❌ Failed to connect to Coinbase WebSocket", e);
                }
            }

            @Override
            public void onMessage(String message) {
                try {
                    // Create MarketDataEvent with the raw message for now
                    MarketDataEvent event = new MarketDataEvent(message, exchangeName);
                    
                    log.debug("✉️ Websocket Message Received");
                    kafkaTemplate.send(kafkaTopic, event);
                    log.debug("✉️ Sent market data event to Kafka topic: {}", kafkaTopic);
                } catch (Exception e) {
                    log.error("❌ Failed to process WebSocket message: {}", message, e);
                }
            }
            @Override
            public void onClose(int code, String reason, boolean remote) {
                try {
                    webSocketClient.close(code, reason);
                    connected = false;
                    log.info("✅ WebSocket connection closed. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
                } catch (Exception e) {
                    log.error("❌ Failed to close websocket client: {}", reason, e);
                }
            }
            @Override
            public void onError(Exception ex) {
                connected = false;
                log.error("❌ There was an error with the Websocket:", ex);
            }
        };
        webSocketClient.connect();
    }


    private String buildSubscribeMessage(List<String> productIds) {
        //todo build out option to sub to other channels
        if (authenticated) {
            return buildAuthenticatedSubscribeMessage(productIds);
        }

        // Convert list to JSON array format
        String productIdsJson =  "[\"" + String.join("\",\"", productIds) + "\"]";

        return String.format("""
        {"type": "subscribe", "channels": [{ "name": "ticker", "product_ids": %s }]}
        """, productIdsJson);
    }
    /**
     * Builds an authenticated subscribe message for the Coinbase WebSocket feed.
     * @return JSON string for subscription with authentication fields
     */
    private String buildAuthenticatedSubscribeMessage(List<String> productIds) {
        try {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String method = "GET";
            String requestPath = "/users/self/verify";
            String prehash = timestamp + method + requestPath;
            String signature = generateSignature(prehash, apiSecret);

            // Convert list to JSON array format
            String productIdsJson = productIds.isEmpty() ?
                    "[\"BTC-USD\",\"ETH-USD\"]" :
                    "[\"" + String.join("\",\"", productIds) + "\"]";

            return String.format("""
            {"type": "subscribe",
             "channels": [{ "name": "ticker", "product_ids": %s }],
             "signature": "%s",
             "key": "%s",
             "passphrase": "%s",
             "timestamp": "%s"
            }
            """, productIdsJson, signature, apiKey, passphrase, timestamp);
        } catch (Exception e) {
            log.error("❌ Failed to build authenticated subscribe message", e);
            throw new RuntimeException("Failed to build authenticated subscribe message", e);
        }
    }

    /**
     * Generates a base64-encoded HMAC SHA256 signature for authentication.
     * @param prehash the string to sign
     * @param secret the API secret
     * @return the base64-encoded signature
     */
    private String generateSignature(String prehash, String secret) throws Exception {
        log.debug("🔄 Generating signature for authentication" );
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
            Mac sha256 = Mac.getInstance("HmacSHA256");
            sha256.init(secretKeySpec);
            byte[] hash = sha256.doFinal(prehash.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("❌ Failed to generate signature", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Disconnects from the Coinbase WebSocket feed.
     */
    @Override
    public void disconnect() {
        if (webSocketClient != null && connected) {
            try {
                log.info("🔄 Closing Coinbase WebSocket connection");
                webSocketClient.close();
                connected = false;
                log.info("✅ Disconnected from Coinbase WebSocket");
            } catch (Exception e) {
                log.error("❌ Failed to close websocket client", e);
            }
        }
    }

    /**
     * Checks if the client is currently connected to the WebSocket feed.
     * @return true if connected, false otherwise
     */
    @Override
    public boolean isConnected() {
        log.debug("🔎 Checking if Coinbase WebSocket is connected: {}", connected);
        if (connected) {
            log.debug("✅ WebSocket is connected");
        } else {
            log.warn("⚠️ WebSocket is not connected");
        }
        return connected;
    }

    /**
     * Sends a custom message to the WebSocket server.
     * @param message the message to send
     */
    @Override
    public void sendMessage(String message) {
        if (webSocketClient != null && connected) {
            log.debug("🔄 Sending message to Coinbase WebSocket: {}", message);
            webSocketClient.send(message);
        } else {
            log.error("❌ WebSocket is not connected, failed to send message: {}", message);
        }
    }

}
