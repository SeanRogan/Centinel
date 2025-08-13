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

```bash
mvn spring-boot:run
```

### Production Mode

```bash
mvn clean package
java -jar target/analysis-0.0.1-SNAPSHOT.jar
```

### Docker

```bash
docker build -t analysis-service .
docker run -p 8080:8080 analysis-service
```

## API Endpoints

### Health Checks

- `GET /api/health` - Basic health status
- `GET /api/health/ready` - Readiness check
- `GET /actuator/health` - Spring Boot Actuator health endpoint

### Example Response

```json
{
  "status": "UP",
  "service": "analysis-service",
  "timestamp": "2024-01-15T10:30:00Z",
  "version": "1.0.0"
}
```

## Kafka Consumer Configuration

The service includes two Kafka listeners:

1. **Batch Listener**: Processes multiple messages at once for better throughput
2. **Single Listener**: Processes individual messages for real-time processing

### Consumer Groups

- `analysis-service-group` - Batch processing
- `analysis-service-group-single` - Single message processing

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
- Custom business metrics

## Development

### Adding New Analysis

To add new analysis capabilities:

1. Extend `MarketDataAnalysisService`
2. Implement analysis logic in `performAnalysis` method
3. Add any required dependencies
4. Configure async execution if needed

### Custom Queries

Add custom TimescaleDB queries to `MarketDataRepository`:

```java
@Query(value = "SELECT time_bucket('1 hour', time) AS bucket, " +
               "AVG(price) as avg_price " +
               "FROM market_data " +
               "WHERE product_id = :productId " +
               "GROUP BY bucket", nativeQuery = true)
List<Object[]> getHourlyAverages(@Param("productId") String productId);
```

## Troubleshooting

### Common Issues

1. **Kafka Connection**: Ensure Kafka is running and accessible
2. **Database Connection**: Verify TimescaleDB credentials and connectivity
3. **Message Deserialization**: Check JSON format matches expected structure
4. **Memory Issues**: Monitor heap usage for large message batches

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.fedelis.centinel.analysis: DEBUG
    org.springframework.kafka: DEBUG
```

## Contributing

1. Follow the existing code style
2. Add tests for new functionality
3. Update documentation
4. Ensure all tests pass before submitting

## License

This project is licensed under the MIT License.