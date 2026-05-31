package com.trading.marketaggregation.event.publisher;

import com.trading.contracts.market.MarketBarEvent;
import com.trading.marketaggregation.application.domain.model.MarketBar;
import com.trading.marketaggregation.application.port.output.MarketBarPublisherPort;
import com.trading.marketaggregation.event.mapper.MarketBarAvroMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class MarketBarKafkaPublisher implements MarketBarPublisherPort {

    private final KafkaTemplate<String, MarketBarEvent> kafkaTemplate;
    private final String topic;

    @Override
    public void publish(MarketBar bar) {
        MarketBarEvent event = MarketBarAvroMapper.toAvro(bar);
        String key = bar.metadata().instrumentId() + ":" + bar.timeframe().code();

        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish market bar: instrumentId={}, timeframe={}, closed={}",
                        bar.metadata().instrumentId(), bar.timeframe().code(), bar.closed(), ex);
                } else {
                    log.debug("Published market bar: instrumentId={}, timeframe={}, closed={}, offset={}",
                        bar.metadata().instrumentId(), bar.timeframe().code(), bar.closed(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}