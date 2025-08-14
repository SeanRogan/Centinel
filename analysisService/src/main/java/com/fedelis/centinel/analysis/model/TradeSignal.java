package com.fedelis.centinel.analysis.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_signals", indexes = {
    @Index(name = "idx_trade_signals_product_id", columnList = "product_id"),
    @Index(name = "idx_trade_signals_signal_type", columnList = "signal_type"),
    @Index(name = "idx_trade_signals_timestamp", columnList = "timestamp DESC"),
    @Index(name = "idx_trade_signals_confidence", columnList = "confidence DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeSignal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "signal_type", nullable = false)
    private String signalType; // BUY, SELL, HOLD
    
    @Column(name = "strategy")
    private String strategy; // RSI, MACD, BOLLINGER_BANDS, etc.
    
    @Column(name = "current_price", precision = 20, scale = 8)
    private BigDecimal currentPrice;
    
    @Column(name = "target_price", precision = 20, scale = 8)
    private BigDecimal targetPrice;
    
    @Column(name = "stop_loss", precision = 20, scale = 8)
    private BigDecimal stopLoss;
    
    @Column(name = "take_profit", precision = 20, scale = 8)
    private BigDecimal takeProfit;
    
    @Column(name = "confidence")
    private Double confidence; // 0.0 to 1.0
    
    @Column(name = "reasoning", length = 1000)
    private String reasoning;
    
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
    
    @Column(name = "source")
    private String source;
    
    // Technical indicator values that triggered the signal
    @Column(name = "rsi_value")
    private Double rsiValue;
    
    @Column(name = "macd_value")
    private Double macdValue;
    
    @Column(name = "macd_signal")
    private Double macdSignal;
    
    @Column(name = "macd_histogram")
    private Double macdHistogram;
    
    @Column(name = "bollinger_upper")
    private Double bollingerUpper;
    
    @Column(name = "bollinger_middle")
    private Double bollingerMiddle;
    
    @Column(name = "bollinger_lower")
    private Double bollingerLower;
    
    @Column(name = "sma_20")
    private Double sma20;
    
    @Column(name = "sma_50")
    private Double sma50;
    
    @Column(name = "ema_12")
    private Double ema12;
    
    @Column(name = "ema_26")
    private Double ema26;
}
