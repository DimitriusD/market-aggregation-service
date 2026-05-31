package com.trading.marketaggregation.application.domain.model;

import com.trading.common.enums.TradeSide;

import java.math.BigDecimal;

public record TradeTick(
        Metadata metadata,
        Long tradeId,
        BigDecimal price,
        BigDecimal quantity,
        TradeSide side
) {
}
