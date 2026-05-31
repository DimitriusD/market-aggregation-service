package com.trading.marketaggregation.application.domain.model;

public enum BarTimeframe {
    ONE_MINUTE("1m");

    private final String code;

    BarTimeframe(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
