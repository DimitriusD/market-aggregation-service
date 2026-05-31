package com.trading.marketaggregation.application.domain.model;

import java.util.List;

public record AggregationResult(
    List<MarketBar> barsToPublish,
    AggregationDecision decision
) {
    public static AggregationResult empty(AggregationDecision decision) {
        return new AggregationResult(List.of(), decision);
    }

    public static AggregationResult publish(List<MarketBar> bars, AggregationDecision decision) {
        return new AggregationResult(List.copyOf(bars), decision);
    }
}