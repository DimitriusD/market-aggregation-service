package com.trading.marketaggregation.application.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BarBucketCalculatorTest {

    private final BarBucketCalculator calculator = new BarBucketCalculator();

    @Test
    void floorToMinuteAtExactMinute() {
        long exactMinute = 1_700_000_040_000L;
        assertThat(calculator.floorToMinute(exactMinute)).isEqualTo(exactMinute);
    }

    @Test
    void floorToMinuteWithOffset() {
        long ts = 1_700_000_040_000L + 31_456L;
        assertThat(calculator.floorToMinute(ts)).isEqualTo(1_700_000_040_000L);
    }

    @Test
    void floorToMinuteAtZero() {
        assertThat(calculator.floorToMinute(0L)).isZero();
    }

    @Test
    void oneMinuteCloseTime() {
        long openTime = 1_700_000_040_000L;
        assertThat(calculator.oneMinuteCloseTime(openTime)).isEqualTo(openTime + 59_999L);
    }
}
