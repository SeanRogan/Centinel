# Analysis Service

The Analysis Service is a Spring Boot application that consumes market data from Kafka topics and performs real-time analysis while persisting data to TimescaleDB.

## Features

- **Kafka Consumer**: Listens to market data events from the monitor service
- **TimescaleDB Integration**: Stores time-series market data in a hypertable
- **Async Analysis**: Performs market data analysis in separate threads
- **Batch Processing**: Supports both single and batch message processing
- **Error Handling**: Robust error handling with retry mechanisms
- **Health Monitoring**: Built-in health check endpoints

## Architecture

```
Monitor Service (Kafka Producer)
           ↓
    Kafka Topic (market-data)
           ↓
Analysis Service (Kafka Consumer)
           ↓
    ┌─────────────┬─────────────┐
    ↓             ↓             ↓
TimescaleDB   Analysis    Health Checks
(Persistence) (Async)     (Monitoring)
```

## Prerequisites

- Java 24+
- Maven 3.8+
- Kafka (running on localhost:9092 by default)
- TimescaleDB/PostgreSQL (running on localhost:5432 by default)

## Configuration

### Environment Variables

| Variable | Default                                     | Description |
|----------|---------------------------------------------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                            | Kafka bootstrap servers |
| `TIMESCALEDB_URL` | `jdbc:postgresql://localhost:5432/centinel` | TimescaleDB connection URL |
| `TIMESCALEDB_USERNAME` | `postgres`                                  | TimescaleDB username |
| `TIMESCALEDB_PASSWORD` | `password`                                  | TimescaleDB password |

### Application Properties

The service uses the following key configurations:

```yaml
kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  consumer:
    group-id: analysis-service-group
    auto-offset-reset: earliest
    enable-auto-commit: false
  topic:
    market-data: market-data

spring:
  datasource:
    url: ${TIMESCALEDB_URL:jdbc:postgresql://localhost:5432/centinel}
    username: ${TIMESCALEDB_USERNAME:postgres}
    password: ${TIMESCALEDB_PASSWORD:password}
```

## Running the Service

### Development Mode
In the parent Centinel directory run:
```bash
docker compose up -d
```

## API Endpoints

## Kafka Consumer Configuration

### Consumer Groups

- `analysis-service-group` - Batch processing

### Error Handling

- Manual acknowledgment for better control
- Exponential backoff for failed messages
- Dead letter queue support (configurable)

## TimescaleDB Schema

The service uses JPA to automatically create the `market_data` table with the following structure:

```sql
CREATE TABLE market_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tick_id UUID,
    type VARCHAR(50),
    sequence INTEGER,
    product_id VARCHAR(50),
    price DECIMAL(20,8),
    open_24h DECIMAL(20,8),
    volume_24h DECIMAL(20,8),
    low_24h DECIMAL(20,8),
    high_24h DECIMAL(20,8),
    volume_30d DECIMAL(20,8),
    best_bid DECIMAL(20,8),
    best_ask DECIMAL(20,8),
    best_bid_size DECIMAL(20,8),
    best_ask_size DECIMAL(20,8),
    side VARCHAR(10),
    time TIMESTAMPTZ NOT NULL,
    trade_id INTEGER,
    last_size DECIMAL(20,8),
    source VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

### Converting to Hypertable

After the application starts and creates the table, run the following SQL to convert it to a TimescaleDB hypertable:

```sql
-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Convert to hypertable
SELECT create_hypertable('market_data', 'time', if_not_exists => TRUE);
```

This provides:
- Time partitioning on the `time` column
- Automatic chunk management
- Optimized indexes for time-series queries

## Data Flow

1. **Message Reception**: Kafka consumer receives market data events
2. **JSON Parsing**: Message content is parsed from JSON to MarketData entity
3. **Persistence**: Data is saved to TimescaleDB hypertable
4. **Analysis**: Async analysis is triggered in separate thread
5. **Acknowledgment**: Message is acknowledged after successful processing

## Monitoring

### Logs

The service provides detailed logging at different levels:
- `DEBUG`: Detailed processing information
- `INFO`: General service events
- `WARN`: Warning conditions
- `ERROR`: Error conditions

### Metrics

Spring Boot Actuator provides metrics for:
- Kafka consumer lag
- Database connection pool
- Application performance

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.fedelis.centinel.analysis: DEBUG
    org.springframework.kafka: DEBUG
```

## License

This project is licensed under the MIT License.