package com.trading.marketaggregation.event.mapper;

import com.trading.contracts.common.MetadataEvent;
import com.trading.contracts.market.MarketBarEvent;
import com.trading.marketaggregation.application.domain.model.MarketBar;

public final class MarketBarAvroMapper {

    private MarketBarAvroMapper() {}

    public static MarketBarEvent toAvro(MarketBar bar) {
        MetadataEvent metadata = new MetadataEvent(
            bar.metadata().schemaVersion(),
            bar.metadata().eventType(),
            bar.metadata().exchange(),
            bar.metadata().marketType(),
            bar.metadata().base(),
            bar.metadata().quote(),
            bar.metadata().symbol(),
            bar.metadata().instrumentId(),
            bar.metadata().eventId(),
            bar.metadata().sourceStream(),
            bar.metadata().exchangeTs(),
            bar.metadata().receivedTs(),
            bar.metadata().processedTs()
        );

        return new MarketBarEvent(
            metadata,
            bar.timeframe().code(),
            bar.openTime(),
            bar.closeTime(),
            bar.open().toPlainString(),
            bar.high().toPlainString(),
            bar.low().toPlainString(),
            bar.close().toPlainString(),
            bar.baseVolume().toPlainString(),
            bar.quoteVolume().toPlainString(),
            bar.tradeCount(),
            bar.closed(),
            bar.firstTradeId(),
            bar.lastTradeId(),
            bar.firstTradeTs(),
            bar.lastTradeTs()
        );
    }
}