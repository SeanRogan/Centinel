package com.fedelis.centinel.analysis.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_data", indexes = {
    @Index(name = "idx_market_data_product_id_time", columnList = "product_id, time DESC"),
    @Index(name = "idx_market_data_source_time", columnList = "source, time DESC"),
    @Index(name = "idx_market_data_time", columnList = "time DESC")
})
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "tick_id")
    private UUID tickId;
    
    @Column(name = "type")
    private String type;
    
    @Column(name = "sequence")
    private Integer sequence;
    
    @Column(name = "product_id")
    private String productId;
    
    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;
    
    @Column(name = "open_24h", precision = 20, scale = 8)
    private BigDecimal open24h;
    
    @Column(name = "volume_24h", precision = 20, scale = 8)
    private BigDecimal volume24h;
    
    @Column(name = "low_24h", precision = 20, scale = 8)
    private BigDecimal low24h;
    
    @Column(name = "high_24h", precision = 20, scale = 8)
    private BigDecimal high24h;
    
    @Column(name = "volume_30d", precision = 20, scale = 8)
    private BigDecimal volume30d;
    
    @Column(name = "best_bid", precision = 20, scale = 8)
    private BigDecimal bestBid;
    
    @Column(name = "best_ask", precision = 20, scale = 8)
    private BigDecimal bestAsk;
    
    @Column(name = "best_bid_size", precision = 20, scale = 8)
    private BigDecimal bestBidSize;
    
    @Column(name = "best_ask_size", precision = 20, scale = 8)
    private BigDecimal bestAskSize;
    
    @Column(name = "side")
    private String side;
    
    @Column(name = "time")
    private Instant time;
    
    @Column(name = "trade_id")
    private Integer tradeId;
    
    @Column(name = "last_size", precision = 20, scale = 8)
    private BigDecimal lastSize;
    
    @Column(name = "source")
    private String source;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
