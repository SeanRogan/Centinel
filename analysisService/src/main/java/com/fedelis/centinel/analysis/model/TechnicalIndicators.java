package com.fedelis.centinel.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicators {
    
    private Double rsi;
    private Double macd;
    private Double macdSignal;
    private Double macdHistogram;
    private Double bollingerUpper;
    private Double bollingerMiddle;
    private Double bollingerLower;
    private Double sma20;
    private Double sma50;
    private Double ema12;
    private Double ema26;
    private Double atr; // Average True Range
    private Double volumeSma;
    private Double priceChange;
    private Double volumeChange;
    
    // Signal strength indicators
    private Double trendStrength;
    private Double volatility;
    private Double momentum;
}
