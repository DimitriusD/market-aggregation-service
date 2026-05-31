package com.trading.marketaggregation.event.mapper;

import com.trading.contracts.common.MetadataEvent;
import com.trading.marketaggregation.application.domain.model.Metadata;

public final class MetadataAvroMapper {

    private MetadataAvroMapper() {}

    public static Metadata toDomain(MetadataEvent avro) {
        if (avro == null) {
            throw new AvroMappingException("metadata is null");
        }

        CharSequence instrumentId = avro.getInstrumentId();
        if (instrumentId == null || instrumentId.toString().isBlank()) {
            throw new AvroMappingException("metadata.instrumentId is blank");
        }

        if (avro.getExchangeTs() <= 0) {
            throw new AvroMappingException("metadata.exchangeTs must be positive, got: " + avro.getExchangeTs());
        }

        return new Metadata(
            avro.getSchemaVersion(),
            nz(avro.getEventType()),
            nz(avro.getExchange()),
            nz(avro.getMarketType()),
            nz(avro.getBase()),
            nz(avro.getQuote()),
            nz(avro.getSymbol()),
            instrumentId.toString(),
            nz(avro.getEventId()),
            nz(avro.getSourceStream()),
            avro.getExchangeTs(),
            avro.getReceivedTs(),
            avro.getProcessedTs()
        );
    }

    private static String nz(CharSequence s) {
        return s != null ? s.toString() : "";
    }
}