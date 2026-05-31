package com.trading.marketaggregation.application.domain.service;

public final class BarBucketCalculator {

    public long floorToMinute(long epochMillis) {
        return epochMillis - (epochMillis % 60_000L);
    }

    public long oneMinuteCloseTime(long openTime) {
        return openTime + 60_000L - 1L;
    }
}
