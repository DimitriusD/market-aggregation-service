package com.trading.marketaggregation.application.domain.model;

public enum AggregationDecision {
    NEW_BAR_STARTED,
    CURRENT_BAR_UPDATED,
    CURRENT_BAR_PARTIAL_PUBLISHED,
    PREVIOUS_BAR_CLOSED_AND_NEW_STARTED,
    LATE_TRADE_IGNORED,
    INVALID_TRADE_SKIPPED
}
