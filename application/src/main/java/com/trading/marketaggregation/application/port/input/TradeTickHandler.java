package com.trading.marketaggregation.application.port.input;

import com.trading.marketaggregation.application.domain.model.TradeTick;

public interface TradeTickHandler {
    void handle(TradeTick trade);
}
