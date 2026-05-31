package com.trading.marketaggregation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aggregation")
public record AggregationProperties(
    long partialPublishIntervalMs,
    String lateTradePolicy
) {
    public AggregationProperties {
        if (partialPublishIntervalMs <= 0) {
            throw new IllegalArgumentException("partialPublishIntervalMs must be positive");
        }
        if (lateTradePolicy == null || lateTradePolicy.isBlank()) {
            throw new IllegalArgumentException("lateTradePolicy must not be blank");
        }
        if (!"IGNORE".equalsIgnoreCase(lateTradePolicy)) {
            throw new IllegalArgumentException("Only IGNORE lateTradePolicy is supported in v1");
        }
    }
}