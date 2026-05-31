package com.trading.marketaggregation.application.port.output;

import com.trading.marketaggregation.application.domain.model.MarketBar;

public interface MarketBarPublisherPort {
    void publish(MarketBar bar);
}