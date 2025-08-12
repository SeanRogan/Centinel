# Exchange Monitor Service

This service connects to cryptocurrency exchange WebSocket feeds and streams market data to Kafka topics for consumption by other services.

## Architecture

The service implements a WebSocket to Kafka pipeline:

1. **WebSocket Connection**: Connects to Coinbase WebSocket feed
2. **Message Processing**: Receives real-time market data messages
3. **Kafka Production**: Sends messages as `MarketDataEvent` objects to Kafka topics
4. **Service Consumption**: Other services can consume from Kafka topics

## Components

### Core Services

- **`CoinbaseDataStreamingService`**: Orchestrates the WebSocket connection and streaming process
- **`CoinbaseWebsocketClient`**: Handles WebSocket connection lifecycle and message processing
- **`KafkaProducerConfig`**: Configures Kafka producer for sending market data events

### Data Model

- **`MarketDataEvent`**: Represents a market data message with source and timestamp

## Configuration

### Application Properties

```yaml
# Market Data Configuration
market:
  data:
    symbols: BTC-USD,ETH-USD,ADA-USD,SOL-USD

# Coinbase API Configuration
coinbase:
  api:
    url:
      public: wss://ws-feed.exchange.coinbase.com
      private: # For authenticated feeds
    key: ${COINBASE_API_KEY:}
    secret: ${COINBASE_API_SECRET:}
    passphrase: ${COINBASE_API_PASSPHRASE:}

# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

kafka:
  topic:
    market-data: market-data
```

### Environment Variables

- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses (default: localhost:9092)
- `COINBASE_API_KEY`: Coinbase API key for authenticated feeds
- `COINBASE_API_SECRET`: Coinbase API secret for authenticated feeds
- `COINBASE_API_PASSPHRASE`: Coinbase API passphrase for authenticated feeds

## Usage

### Starting the Service

The service automatically starts streaming when the application starts. The `ApplicationConfig` class listens for the `ApplicationReadyEvent` and triggers the streaming process.

### Manual Control

```java
@Autowired
private CoinbaseDataStreamingService streamingService;

// Start streaming
CompletableFuture<Void> future = streamingService.startStreaming();

// Stop streaming
streamingService.stopStreaming();
```

### Kafka Topics

The service produces messages to the `market-data` topic (configurable via `kafka.topic.market-data`).

### Message Format

```json
{
  "message": "{\"type\":\"ticker\",\"product_id\":\"BTC-USD\",\"price\":\"50000.00\"}",
  "source": "coinbase",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Recent Fixes

### Issues Resolved

1. **Missing Application Startup**: Added `@EventListener(ApplicationReadyEvent.class)` to automatically start streaming
2. **Configuration Property Mismatch**: Fixed property injection for comma-separated symbols
3. **WebSocket URL Configuration**: Corrected field names and URL mappings
4. **Double Connection Issue**: Removed redundant `connect()` calls
5. **Kafka Topic Configuration**: Added configurable Kafka topic names
6. **Error Handling**: Improved error handling and logging

### Code Changes

- **`CoinbaseDataStreamingService`**: Fixed symbol parsing and removed double connection
- **`CoinbaseWebsocketClient`**: Corrected URL configuration and added Kafka topic config
- **`ApplicationConfig`**: Added automatic startup trigger
- **Configuration Files**: Added Kafka topic configuration

## Testing

### Unit Tests

- `CoinbaseDataStreamingServiceTest`: Tests service lifecycle and error handling
- `KafkaProducerConfigTest`: Verifies Kafka configuration

### Running Tests

```bash
mvn test
```

## Monitoring

The service includes Spring Boot Actuator endpoints for monitoring:

- Health check: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`

## Dependencies

- Spring Boot 3.x
- Spring Kafka
- Java-WebSocket
- Lombok
- Jackson

## Development

### Prerequisites

- Java 17+
- Maven 3.6+
- Kafka broker running
- Docker (for testing with Testcontainers)

### Local Development

1. Start Kafka broker
2. Set environment variables if needed
3. Run the application: `mvn spring-boot:run`

### Docker

```bash
docker-compose up
```