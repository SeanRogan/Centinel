-- Enable TimescaleDB extension if not already enabled
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create the market_data table if it doesn't exist
CREATE TABLE IF NOT EXISTS market_data (
    id BIGSERIAL,
    symbol TEXT NOT NULL,
    price DECIMAL(20, 8) NOT NULL,
    volume DECIMAL(20, 8),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    exchange TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    PRIMARY KEY (id, timestamp)
);

-- Create index on timestamp for better performance
CREATE INDEX IF NOT EXISTS idx_market_data_timestamp ON market_data (timestamp);

-- Convert the market_data table to a hypertable
SELECT create_hypertable('market_data', 'timestamp', if_not_exists => TRUE);

-- Add compression policy (optional - for older data)
-- SELECT add_compression_policy('market_data', INTERVAL '7 days');

-- Add retention policy (optional - keep data for 1 year)
-- SELECT add_retention_policy('market_data', INTERVAL '1 year');
