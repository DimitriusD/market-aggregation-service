package com.trading.marketaggregation.application.domain.model;

import java.math.BigDecimal;

public record MarketBar(
    Metadata metadata,

    BarTimeframe timeframe,

    long openTime,
    long closeTime,

    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,

    BigDecimal baseVolume,
    BigDecimal quoteVolume,

    long tradeCount,

    boolean closed,

    Long firstTradeId,
    Long lastTradeId,

    long firstTradeTs,
    long lastTradeTs
) {}