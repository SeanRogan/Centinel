package com.fedelis.centinel.analysis.repository;

import com.fedelis.centinel.analysis.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, UUID> {

    List<MarketData> findByProductIdOrderByTimeDesc(String productId);
    
    List<MarketData> findByProductIdAndTimeBetweenOrderByTimeDesc(
        String productId, 
        Instant startTime, 
        Instant endTime
    );
    
    @Query("SELECT m FROM MarketData m WHERE m.productId = :productId AND m.time >= :startTime ORDER BY m.time DESC")
    List<MarketData> findRecentDataByProductId(
        @Param("productId") String productId, 
        @Param("startTime") Instant startTime
    );
    
    @Query(value = "SELECT * FROM market_data WHERE product_id = :productId AND time >= :startTime ORDER BY time DESC LIMIT :limit", nativeQuery = true)
    List<MarketData> findRecentDataByProductIdWithLimit(
        @Param("productId") String productId, 
        @Param("startTime") Instant startTime,
        @Param("limit") int limit
    );
    
    // TimescaleDB specific queries for time-series analysis
    @Query(value = "SELECT time_bucket('1 minute', time) AS bucket, " +
                   "AVG(price) as avg_price, " +
                   "MAX(price) as max_price, " +
                   "MIN(price) as min_price, " +
                   "SUM(volume_24h) as total_volume " +
                   "FROM market_data " +
                   "WHERE product_id = :productId AND time >= :startTime " +
                   "GROUP BY bucket " +
                   "ORDER BY bucket DESC", nativeQuery = true)
    List<Object[]> getTimeSeriesData(
        @Param("productId") String productId, 
        @Param("startTime") Instant startTime
    );
    
    @Query(value = "SELECT * FROM market_data WHERE product_id = :productId ORDER BY time DESC LIMIT 1", nativeQuery = true)
    MarketData findLatestByProductId(@Param("productId") String productId);
}
