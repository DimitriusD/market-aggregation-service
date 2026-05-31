package com.trading.marketaggregation.application.domain.service;

import com.trading.marketaggregation.application.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeOneMinuteBarAggregatorTest {

    private static final long MINUTE_MS = 60_000L;
    private static final long BASE_TIME = 1_700_000_000_000L;
    private static final long BUCKET_OPEN = BASE_TIME - (BASE_TIME % MINUTE_MS);

    private RealtimeOneMinuteBarAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new RealtimeOneMinuteBarAggregator(new BarBucketCalculator(), new BarPublishPolicy(1000));
    }

    @Test
    void firstTradeCreatesNewCandle() {
        TradeTick trade = trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("0.5"), BUCKET_OPEN + 1_000);

        AggregationResult result = aggregator.onTrade(trade, BUCKET_OPEN + 1_000);

        assertThat(result.decision()).isEqualTo(AggregationDecision.NEW_BAR_STARTED);
        assertThat(result.barsToPublish()).hasSize(1);

        MarketBar bar = result.barsToPublish().get(0);
        assertThat(bar.closed()).isFalse();
        assertThat(bar.open()).isEqualByComparingTo("100");
        assertThat(bar.high()).isEqualByComparingTo("100");
        assertThat(bar.low()).isEqualByComparingTo("100");
        assertThat(bar.close()).isEqualByComparingTo("100");
        assertThat(bar.baseVolume()).isEqualByComparingTo("0.5");
        assertThat(bar.tradeCount()).isEqualTo(1);
        assertThat(bar.timeframe()).isEqualTo(BarTimeframe.ONE_MINUTE);
        assertThat(bar.metadata().instrumentId()).isEqualTo("BINANCE:SPOT:BTCUSDT");
        assertThat(bar.metadata().eventType()).isEqualTo("MARKET_BAR");
        assertThat(bar.metadata().sourceStream()).isEqualTo("market-aggregation-service");
    }

    @Test
    void multipleTradesInSameMinuteUpdateOhlcv() {
        long nowMs = BUCKET_OPEN + 1_000;

        aggregator.onTrade(trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("0.5"), BUCKET_OPEN + 1_000), nowMs);
        nowMs += 2000;
        aggregator.onTrade(trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("102"), new BigDecimal("0.2"), BUCKET_OPEN + 20_000), nowMs);
        nowMs += 2000;
        AggregationResult result = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("99"), new BigDecimal("0.3"), BUCKET_OPEN + 55_000), nowMs);

        assertThat(result.barsToPublish()).hasSize(1);
        MarketBar bar = result.barsToPublish().get(0);

        assertThat(bar.open()).isEqualByComparingTo("100");
        assertThat(bar.high()).isEqualByComparingTo("102");
        assertThat(bar.low()).isEqualByComparingTo("99");
        assertThat(bar.close()).isEqualByComparingTo("99");
        assertThat(bar.baseVolume()).isEqualByComparingTo("1.0");
        assertThat(bar.tradeCount()).isEqualTo(3);
    }

    @Test
    void tradeInNextMinuteClosesPreviousCandle() {
        aggregator.onTrade(trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + 55_000), BUCKET_OPEN + 55_000);

        long nowMs = BUCKET_OPEN + MINUTE_MS + 3_000;
        AggregationResult result = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("101"), new BigDecimal("1"), BUCKET_OPEN + MINUTE_MS + 3_000), nowMs);

        assertThat(result.decision()).isEqualTo(AggregationDecision.PREVIOUS_BAR_CLOSED_AND_NEW_STARTED);
        assertThat(result.barsToPublish()).hasSizeGreaterThanOrEqualTo(1);

        MarketBar closedBar = result.barsToPublish().get(0);
        assertThat(closedBar.closed()).isTrue();
        assertThat(closedBar.close()).isEqualByComparingTo("100");
        assertThat(closedBar.openTime()).isEqualTo(BUCKET_OPEN);

        if (result.barsToPublish().size() > 1) {
            MarketBar partialBar = result.barsToPublish().get(1);
            assertThat(partialBar.closed()).isFalse();
            assertThat(partialBar.open()).isEqualByComparingTo("101");
            assertThat(partialBar.openTime()).isEqualTo(BUCKET_OPEN + MINUTE_MS);
        }
    }

    @Test
    void lateTradeIgnored() {
        long nowMs = BUCKET_OPEN + MINUTE_MS + 5_000;
        aggregator.onTrade(trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + MINUTE_MS + 5_000), nowMs);

        AggregationResult result = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("99"), new BigDecimal("1"), BUCKET_OPEN + 50_000), nowMs + 1000);

        assertThat(result.decision()).isEqualTo(AggregationDecision.LATE_TRADE_IGNORED);
        assertThat(result.barsToPublish()).isEmpty();
    }

    @Test
    void partialPublishThrottling() {
        long nowMs = BUCKET_OPEN + 1_000;

        AggregationResult r1 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + 1_000), nowMs);
        assertThat(r1.barsToPublish()).hasSize(1);

        AggregationResult r2 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("101"), new BigDecimal("1"), BUCKET_OPEN + 1_100), nowMs + 100);
        assertThat(r2.decision()).isEqualTo(AggregationDecision.CURRENT_BAR_UPDATED);
        assertThat(r2.barsToPublish()).isEmpty();

        AggregationResult r3 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("102"), new BigDecimal("1"), BUCKET_OPEN + 1_200), nowMs + 200);
        assertThat(r3.decision()).isEqualTo(AggregationDecision.CURRENT_BAR_UPDATED);
        assertThat(r3.barsToPublish()).isEmpty();

        AggregationResult r4 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("103"), new BigDecimal("1"), BUCKET_OPEN + 2_500), nowMs + 1500);
        assertThat(r4.decision()).isEqualTo(AggregationDecision.CURRENT_BAR_PARTIAL_PUBLISHED);
        assertThat(r4.barsToPublish()).hasSize(1);
        assertThat(r4.barsToPublish().get(0).closed()).isFalse();
    }

    @Test
    void closedCandleAlwaysPublished() {
        long nowMs = BUCKET_OPEN + 1_000;
        aggregator.onTrade(trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + 1_000), nowMs);

        AggregationResult result = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("101"), new BigDecimal("1"), BUCKET_OPEN + MINUTE_MS + 100), nowMs + 200);

        assertThat(result.decision()).isEqualTo(AggregationDecision.PREVIOUS_BAR_CLOSED_AND_NEW_STARTED);

        MarketBar closedBar = result.barsToPublish().stream()
            .filter(MarketBar::closed)
            .findFirst()
            .orElseThrow();
        assertThat(closedBar.closed()).isTrue();
        assertThat(closedBar.close()).isEqualByComparingTo("100");
    }

    @Test
    void quoteVolumeIsCalculatedAsPriceTimesQuantity() {
        long nowMs = BUCKET_OPEN + 1_000;

        AggregationResult r1 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("2"), BUCKET_OPEN + 1_000), nowMs);
        assertThat(r1.barsToPublish().get(0).quoteVolume()).isEqualByComparingTo("200");

        nowMs += 1500;
        AggregationResult r2 = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("101"), new BigDecimal("3"), BUCKET_OPEN + 2_500), nowMs);
        assertThat(r2.barsToPublish()).hasSize(1);
        // 100*2 + 101*3 = 200 + 303 = 503
        assertThat(r2.barsToPublish().get(0).quoteVolume()).isEqualByComparingTo("503");
    }

    @Test
    void invalidTradeSkipped() {
        TradeTick noMetadata = new TradeTick(null, null, new BigDecimal("100"), new BigDecimal("1"), null);
        assertThat(aggregator.onTrade(noMetadata, BUCKET_OPEN + 1_000).decision())
            .isEqualTo(AggregationDecision.INVALID_TRADE_SKIPPED);

        TradeTick noPrice = new TradeTick(validMetadata("BINANCE:SPOT:BTCUSDT", BUCKET_OPEN + 1_000),
            null, null, new BigDecimal("1"), null);
        assertThat(aggregator.onTrade(noPrice, BUCKET_OPEN + 1_000).decision())
            .isEqualTo(AggregationDecision.INVALID_TRADE_SKIPPED);

        TradeTick noQuantity = new TradeTick(validMetadata("BINANCE:SPOT:BTCUSDT", BUCKET_OPEN + 1_000),
            null, new BigDecimal("100"), null, null);
        assertThat(aggregator.onTrade(noQuantity, BUCKET_OPEN + 1_000).decision())
            .isEqualTo(AggregationDecision.INVALID_TRADE_SKIPPED);

        TradeTick noInstrument = new TradeTick(validMetadata("", BUCKET_OPEN + 1_000),
            null, new BigDecimal("100"), new BigDecimal("1"), null);
        assertThat(aggregator.onTrade(noInstrument, BUCKET_OPEN + 1_000).decision())
            .isEqualTo(AggregationDecision.INVALID_TRADE_SKIPPED);

        TradeTick noTs = new TradeTick(validMetadata("BINANCE:SPOT:BTCUSDT", 0),
            null, new BigDecimal("100"), new BigDecimal("1"), null);
        assertThat(aggregator.onTrade(noTs, BUCKET_OPEN + 1_000).decision())
            .isEqualTo(AggregationDecision.INVALID_TRADE_SKIPPED);
    }

    @Test
    void multipleInstrumentsIndependent() {
        long nowMs = BUCKET_OPEN + 1_000;

        AggregationResult rBtc = aggregator.onTrade(
            trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + 1_000), nowMs);
        AggregationResult rEth = aggregator.onTrade(
            trade("BINANCE:SPOT:ETHUSDT", new BigDecimal("3000"), new BigDecimal("10"), BUCKET_OPEN + 1_000), nowMs);

        assertThat(rBtc.barsToPublish().get(0).metadata().instrumentId()).isEqualTo("BINANCE:SPOT:BTCUSDT");
        assertThat(rEth.barsToPublish().get(0).metadata().instrumentId()).isEqualTo("BINANCE:SPOT:ETHUSDT");
        assertThat(rBtc.barsToPublish().get(0).close()).isEqualByComparingTo("100");
        assertThat(rEth.barsToPublish().get(0).close()).isEqualByComparingTo("3000");
    }

    @Test
    void firstAndLastTradeTimestampsTracked() {
        long nowMs = BUCKET_OPEN + 1_000;

        TradeTick t1 = trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("100"), new BigDecimal("1"), BUCKET_OPEN + 5_000);
        aggregator.onTrade(t1, nowMs);

        nowMs += 1500;
        TradeTick t2 = trade("BINANCE:SPOT:BTCUSDT", new BigDecimal("101"), new BigDecimal("1"), BUCKET_OPEN + 50_000);
        AggregationResult result = aggregator.onTrade(t2, nowMs);

        MarketBar bar = result.barsToPublish().get(0);
        assertThat(bar.firstTradeTs()).isEqualTo(BUCKET_OPEN + 5_000);
        assertThat(bar.lastTradeTs()).isEqualTo(BUCKET_OPEN + 50_000);
    }

    private TradeTick trade(String instrumentId, BigDecimal price, BigDecimal quantity, long exchangeTs) {
        String symbol = instrumentId.contains(":") ? instrumentId.substring(instrumentId.lastIndexOf(":") + 1) : instrumentId;
        Metadata metadata = new Metadata(
            1, "TRADE_TICK", "BINANCE", "SPOT", "BTC", "USDT",
            symbol, instrumentId, "event-" + exchangeTs, "canonical.market.trade.v1",
            exchangeTs, exchangeTs, exchangeTs
        );
        return new TradeTick(metadata, null, price, quantity, null);
    }

    private Metadata validMetadata(String instrumentId, long exchangeTs) {
        return new Metadata(
            1, "TRADE_TICK", "BINANCE", "SPOT", "BTC", "USDT",
            "BTCUSDT", instrumentId, "event-1", "canonical.market.trade.v1",
            exchangeTs, exchangeTs, exchangeTs
        );
    }
}