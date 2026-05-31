package com.trading.marketaggregation.application.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OneMinuteBarStateTest {

    private static final long OPEN_TIME = 1_700_000_040_000L;

    @Test
    void startCreatesCorrectInitialState() {
        TradeTick trade = trade(100L, new BigDecimal("50000.50"), new BigDecimal("1.5"), OPEN_TIME + 1_000);

        OneMinuteBarState state = OneMinuteBarState.start(trade, OPEN_TIME);
        MarketBar bar = state.toBar(false, OPEN_TIME + 2_000);

        assertThat(state.openTime()).isEqualTo(OPEN_TIME);
        assertThat(bar.open()).isEqualByComparingTo("50000.50");
        assertThat(bar.high()).isEqualByComparingTo("50000.50");
        assertThat(bar.low()).isEqualByComparingTo("50000.50");
        assertThat(bar.close()).isEqualByComparingTo("50000.50");
        assertThat(bar.baseVolume()).isEqualByComparingTo("1.5");
        assertThat(bar.quoteVolume()).isEqualByComparingTo("75000.750");
        assertThat(bar.tradeCount()).isEqualTo(1);
        assertThat(bar.firstTradeId()).isEqualTo(100L);
        assertThat(bar.lastTradeId()).isEqualTo(100L);
    }

    @Test
    void applyUpdatesOhlcv() {
        TradeTick t1 = trade(1L, new BigDecimal("100"), new BigDecimal("1"), OPEN_TIME + 1_000);
        OneMinuteBarState state = OneMinuteBarState.start(t1, OPEN_TIME);

        TradeTick t2 = trade(2L, new BigDecimal("105"), new BigDecimal("2"), OPEN_TIME + 10_000);
        state.apply(t2);

        TradeTick t3 = trade(3L, new BigDecimal("98"), new BigDecimal("0.5"), OPEN_TIME + 20_000);
        state.apply(t3);

        MarketBar bar = state.toBar(false, OPEN_TIME + 25_000);

        assertThat(bar.open()).isEqualByComparingTo("100");
        assertThat(bar.high()).isEqualByComparingTo("105");
        assertThat(bar.low()).isEqualByComparingTo("98");
        assertThat(bar.close()).isEqualByComparingTo("98");
        assertThat(bar.baseVolume()).isEqualByComparingTo("3.5");
        assertThat(bar.tradeCount()).isEqualTo(3);
        assertThat(bar.lastTradeId()).isEqualTo(3L);
    }

    @Test
    void toBarProducesCorrectMarketBar() {
        TradeTick trade = trade(42L, new BigDecimal("100"), new BigDecimal("1"), OPEN_TIME + 1_000);

        OneMinuteBarState state = OneMinuteBarState.start(trade, OPEN_TIME);
        long computedTs = OPEN_TIME + 5_000;
        MarketBar bar = state.toBar(false, computedTs);

        assertThat(bar.metadata().instrumentId()).isEqualTo("BINANCE:SPOT:BTCUSDT");
        assertThat(bar.metadata().eventType()).isEqualTo("MARKET_BAR");
        assertThat(bar.metadata().sourceStream()).isEqualTo("market-aggregation-service");
        assertThat(bar.metadata().schemaVersion()).isEqualTo(1);
        assertThat(bar.metadata().processedTs()).isEqualTo(computedTs);
        assertThat(bar.timeframe()).isEqualTo(BarTimeframe.ONE_MINUTE);
        assertThat(bar.openTime()).isEqualTo(OPEN_TIME);
        assertThat(bar.closeTime()).isEqualTo(OPEN_TIME + 59_999);
        assertThat(bar.closed()).isFalse();
        assertThat(bar.firstTradeId()).isEqualTo(42L);
        assertThat(bar.firstTradeTs()).isEqualTo(OPEN_TIME + 1_000);
        assertThat(bar.lastTradeTs()).isEqualTo(OPEN_TIME + 1_000);
    }

    @Test
    void belongsToAndComparisons() {
        TradeTick trade = trade(null, new BigDecimal("100"), new BigDecimal("1"), OPEN_TIME + 1_000);
        OneMinuteBarState state = OneMinuteBarState.start(trade, OPEN_TIME);

        assertThat(state.belongsTo(OPEN_TIME)).isTrue();
        assertThat(state.belongsTo(OPEN_TIME + 60_000)).isFalse();
        assertThat(state.isOlderThan(OPEN_TIME + 60_000)).isTrue();
        assertThat(state.isOlderThan(OPEN_TIME)).isFalse();
        assertThat(state.isNewerThan(OPEN_TIME - 60_000)).isTrue();
        assertThat(state.isNewerThan(OPEN_TIME)).isFalse();
    }

    @Test
    void partialPublishTracking() {
        TradeTick trade = trade(null, new BigDecimal("100"), new BigDecimal("1"), OPEN_TIME + 1_000);
        OneMinuteBarState state = OneMinuteBarState.start(trade, OPEN_TIME);

        assertThat(state.lastPartialPublishedTs()).isZero();

        state.markPartialPublished(OPEN_TIME + 2_000);
        assertThat(state.lastPartialPublishedTs()).isEqualTo(OPEN_TIME + 2_000);
    }

    private static TradeTick trade(Long tradeId, BigDecimal price, BigDecimal quantity, long exchangeTs) {
        Metadata metadata = new Metadata(
            1, "TRADE_TICK", "BINANCE", "SPOT", "BTC", "USDT", "BTCUSDT",
            "BINANCE:SPOT:BTCUSDT",
            tradeId != null ? "event-" + tradeId : "event-0",
            "canonical.market.trade.v1",
            exchangeTs, exchangeTs, exchangeTs
        );
        return new TradeTick(metadata, tradeId, price, quantity, null);
    }
}