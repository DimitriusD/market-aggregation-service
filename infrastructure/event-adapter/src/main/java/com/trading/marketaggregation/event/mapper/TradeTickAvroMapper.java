package com.trading.marketaggregation.event.mapper;

import com.trading.common.enums.TradeSide;
import com.trading.contracts.market.TradeTickEvent;
import com.trading.marketaggregation.application.domain.model.TradeTick;

import java.math.BigDecimal;

public final class TradeTickAvroMapper {

    private TradeTickAvroMapper() {}

    public static TradeTick toDomain(TradeTickEvent avro) {
        BigDecimal price = requiredDecimal(avro.getPrice(), "price");
        BigDecimal quantity = requiredDecimal(avro.getQuantity(), "quantity");

        Long tradeId = avro.getTradeId() > 0 ? avro.getTradeId() : null;

        TradeSide side = parseSide(avro.getSide());

        return new TradeTick(
            MetadataAvroMapper.toDomain(avro.getMetadata()),
            tradeId,
            price,
            quantity,
            side
        );
    }

    private static TradeSide parseSide(CharSequence raw) {
        if (raw == null || raw.toString().isBlank()) {
            return null;
        }
        String normalized = raw.toString().trim().toUpperCase();
        try {
            return TradeSide.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal requiredDecimal(CharSequence s, String fieldName) {
        BigDecimal value = decimal(s, fieldName);
        if (value == null) {
            throw new AvroMappingException("trade " + fieldName + " is required");
        }
        return value;
    }

    private static BigDecimal decimal(CharSequence s, String fieldName) {
        if (s == null || s.toString().isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.toString());
        } catch (NumberFormatException e) {
            throw new AvroMappingException("invalid decimal for " + fieldName + ": " + s, e);
        }
    }
}