package com.fedelis.centinel.monitor.entities;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    private Instant timestamp;

    private Instant createdAt;

    private Instant lastUpdatedAt;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "exchange", nullable = false)
    private String exchange;

    @Column(name = "price", precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "volume", precision = 20, scale = 8)
    private BigDecimal volume;

    @Column(name = "bid", precision = 20, scale = 8)
    private BigDecimal bid;

    @Column(name = "ask", precision = 20, scale = 8)
    private BigDecimal ask;

    @Column(name = "high_24h", precision = 20, scale = 8)
    private BigDecimal high24h;

    @Column(name = "low_24h", precision = 20, scale = 8)
    private BigDecimal low24h;

    @Column(name = "open_24h", precision = 20, scale = 8)
    private BigDecimal open24h;

    @Column(name = "raw_data", columnDefinition = "jsonb")

    @JdbcTypeCode(SqlTypes.JSON)
    private String rawData;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    @PreUpdate
    private void onUpdate() {
        lastUpdatedAt = Instant.now();
    }

}
