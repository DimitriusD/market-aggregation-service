package com.trading.marketaggregation.application.domain.service;

import com.trading.marketaggregation.application.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class RealtimeOneMinuteBarAggregator {

    private static final Logger log = LoggerFactory.getLogger(RealtimeOneMinuteBarAggregator.class);

    private final ConcurrentHashMap<String, OneMinuteBarState> statesByInstrument = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locksByInstrument = new ConcurrentHashMap<>();
    private final BarBucketCalculator bucketCalculator;
    private final BarPublishPolicy publishPolicy;

    public RealtimeOneMinuteBarAggregator(BarBucketCalculator bucketCalculator, BarPublishPolicy publishPolicy) {
        this.bucketCalculator = bucketCalculator;
        this.publishPolicy = publishPolicy;
    }

    public AggregationResult onTrade(TradeTick trade, long nowMs) {
        if (!isValid(trade)) {
            String instrumentId = trade != null && trade.metadata() != null
                ? trade.metadata().instrumentId() : null;
            log.warn("Invalid trade skipped: instrumentId={}", instrumentId);
            return AggregationResult.empty(AggregationDecision.INVALID_TRADE_SKIPPED);
        }

        String instrumentId = trade.metadata().instrumentId();
        Object lock = locksByInstrument.computeIfAbsent(instrumentId, key -> new Object());
        synchronized (lock) {
            return doOnTrade(trade, nowMs);
        }
    }

    private AggregationResult doOnTrade(TradeTick trade, long nowMs) {
        String instrumentId = trade.metadata().instrumentId();
        long bucketOpenTime = bucketCalculator.floorToMinute(trade.metadata().exchangeTs());
        OneMinuteBarState state = statesByInstrument.get(instrumentId);

        if (state == null) {
            return handleNewBar(trade, bucketOpenTime, nowMs);
        }

        if (state.belongsTo(bucketOpenTime)) {
            return handleSameBucket(state, trade, nowMs);
        }

        if (state.isOlderThan(bucketOpenTime)) {
            return handleNextBucket(state, trade, bucketOpenTime, nowMs);
        }

        log.debug("Late trade ignored: instrumentId={}, tradeTs={}, currentOpenTime={}",
            instrumentId, trade.metadata().exchangeTs(), state.openTime());
        return AggregationResult.empty(AggregationDecision.LATE_TRADE_IGNORED);
    }

    private AggregationResult handleNewBar(TradeTick trade, long bucketOpenTime, long nowMs) {
        String instrumentId = trade.metadata().instrumentId();
        OneMinuteBarState newState = OneMinuteBarState.start(trade, bucketOpenTime);
        statesByInstrument.put(instrumentId, newState);

        log.debug("New bar started: instrumentId={}, openTime={}", instrumentId, bucketOpenTime);

        if (publishPolicy.shouldPublishPartial(newState, nowMs)) {
            MarketBar bar = newState.toBar(false, nowMs);
            newState.markPartialPublished(nowMs);
            return AggregationResult.publish(List.of(bar), AggregationDecision.NEW_BAR_STARTED);
        }

        return AggregationResult.empty(AggregationDecision.NEW_BAR_STARTED);
    }

    private AggregationResult handleSameBucket(OneMinuteBarState state, TradeTick trade, long nowMs) {
        state.apply(trade);

        if (publishPolicy.shouldPublishPartial(state, nowMs)) {
            MarketBar bar = state.toBar(false, nowMs);
            state.markPartialPublished(nowMs);
            return AggregationResult.publish(List.of(bar), AggregationDecision.CURRENT_BAR_PARTIAL_PUBLISHED);
        }

        return AggregationResult.empty(AggregationDecision.CURRENT_BAR_UPDATED);
    }

    private AggregationResult handleNextBucket(OneMinuteBarState state, TradeTick trade,
                                                long bucketOpenTime, long nowMs) {
        String instrumentId = trade.metadata().instrumentId();
        MarketBar closedBar = state.toBar(true, nowMs);
        log.info("Bar closed: instrumentId={}, openTime={}, close={}, tradeCount={}",
            instrumentId, state.openTime(), state.close(), state.tradeCount());

        OneMinuteBarState newState = OneMinuteBarState.start(trade, bucketOpenTime);
        statesByInstrument.put(instrumentId, newState);

        List<MarketBar> bars = new ArrayList<>();
        bars.add(closedBar);

        if (publishPolicy.shouldPublishPartial(newState, nowMs)) {
            MarketBar partialBar = newState.toBar(false, nowMs);
            newState.markPartialPublished(nowMs);
            bars.add(partialBar);
        }

        return AggregationResult.publish(bars, AggregationDecision.PREVIOUS_BAR_CLOSED_AND_NEW_STARTED);
    }

    private boolean isValid(TradeTick trade) {
        if (trade == null || trade.metadata() == null) {
            return false;
        }
        return trade.metadata().instrumentId() != null
            && !trade.metadata().instrumentId().isBlank()
            && trade.price() != null
            && trade.quantity() != null
            && trade.price().signum() > 0
            && trade.quantity().signum() > 0
            && trade.metadata().exchangeTs() > 0;
    }
}