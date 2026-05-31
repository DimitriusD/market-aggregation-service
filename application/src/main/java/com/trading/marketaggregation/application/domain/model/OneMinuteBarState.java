package com.trading.marketaggregation.application.domain.model;

import java.math.BigDecimal;

public final class OneMinuteBarState {

    private final String exchange;
    private final String marketType;
    private final String base;
    private final String quote;
    private final String symbol;
    private final String instrumentId;
    private final long openTime;
    private final long closeTime;

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal baseVolume;
    private BigDecimal quoteVolume;
    private long tradeCount;
    private Long firstTradeId;
    private Long lastTradeId;
    private long firstTradeTs;
    private long lastTradeTs;
    private long lastReceivedTs;
    private long lastPartialPublishedTs;

    private OneMinuteBarState(TradeTick trade, long openTime) {
        this.exchange = trade.metadata().exchange();
        this.marketType = trade.metadata().marketType();
        this.base = trade.metadata().base();
        this.quote = trade.metadata().quote();
        this.symbol = trade.metadata().symbol();
        this.instrumentId = trade.metadata().instrumentId();
        this.openTime = openTime;
        this.closeTime = openTime + 60_000L - 1L;

        this.open = trade.price();
        this.high = trade.price();
        this.low = trade.price();
        this.close = trade.price();
        this.baseVolume = trade.quantity();
        this.quoteVolume = trade.price().multiply(trade.quantity());
        this.tradeCount = 1;
        this.firstTradeId = trade.tradeId();
        this.lastTradeId = trade.tradeId();
        this.firstTradeTs = trade.metadata().exchangeTs();
        this.lastTradeTs = trade.metadata().exchangeTs();
        this.lastReceivedTs = trade.metadata().receivedTs();
        this.lastSourceProcessedTs = trade.metadata().processedTs();
        this.lastPartialPublishedTs = 0L;
    }

    public static OneMinuteBarState start(TradeTick trade, long openTime) {
        return new OneMinuteBarState(trade, openTime);
    }

    public void apply(TradeTick trade) {
        this.high = this.high.max(trade.price());
        this.low = this.low.min(trade.price());
        this.close = trade.price();
        this.baseVolume = this.baseVolume.add(trade.quantity());
        this.quoteVolume = this.quoteVolume.add(trade.price().multiply(trade.quantity()));
        this.tradeCount++;
        this.lastTradeId = trade.tradeId();
        this.lastTradeTs = trade.metadata().exchangeTs();
        this.lastReceivedTs = trade.metadata().receivedTs();
        this.lastSourceProcessedTs = trade.metadata().processedTs();
    }

    public MarketBar toBar(boolean closed, long computedTs) {
        Metadata metadata = new Metadata(
            1,
            "MARKET_BAR",
            this.exchange,
            this.marketType,
            this.base,
            this.quote,
            this.symbol,
            this.instrumentId,
            buildEventId(closed, computedTs),
            "market-aggregation-service",
            this.lastTradeTs,
            this.lastReceivedTs,
            computedTs
        );

        return new MarketBar(
            metadata,
            BarTimeframe.ONE_MINUTE,
            this.openTime,
            this.closeTime,
            this.open,
            this.high,
            this.low,
            this.close,
            this.baseVolume,
            this.quoteVolume,
            this.tradeCount,
            closed,
            this.firstTradeId,
            this.lastTradeId,
            this.firstTradeTs,
            this.lastTradeTs
        );
    }

    private String buildEventId(boolean closed, long computedTs) {
        return this.instrumentId + ":1m:" + this.openTime + ":" + (closed ? "closed" : "partial") + ":" + computedTs;
    }

    public boolean belongsTo(long bucketOpenTime) {
        return this.openTime == bucketOpenTime;
    }

    public boolean isOlderThan(long bucketOpenTime) {
        return this.openTime < bucketOpenTime;
    }

    public boolean isNewerThan(long bucketOpenTime) {
        return this.openTime > bucketOpenTime;
    }

    public long lastPartialPublishedTs() {
        return lastPartialPublishedTs;
    }

    public void markPartialPublished(long publishedTs) {
        this.lastPartialPublishedTs = publishedTs;
    }

    public long openTime() {
        return openTime;
    }

    public BigDecimal close() {
        return close;
    }

    public long tradeCount() {
        return tradeCount;
    }
}