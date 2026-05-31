package com.trading.marketaggregation.application.domain.service;

import com.trading.marketaggregation.application.domain.model.OneMinuteBarState;

public final class BarPublishPolicy {

    private final long partialPublishIntervalMs;

    public BarPublishPolicy(long partialPublishIntervalMs) {
        if (partialPublishIntervalMs <= 0) {
            throw new IllegalArgumentException("partialPublishIntervalMs must be positive");
        }
        this.partialPublishIntervalMs = partialPublishIntervalMs;
    }

    public boolean shouldPublishPartial(OneMinuteBarState state, long nowMs) {
        return state.lastPartialPublishedTs() == 0L
            || nowMs - state.lastPartialPublishedTs() >= partialPublishIntervalMs;
    }

    public boolean shouldPublishClosed() {
        return true;
    }
}
