package com.trading.marketaggregation.event.consumer;

import com.trading.contracts.market.TradeTickEvent;
import com.trading.marketaggregation.application.domain.model.TradeTick;
import com.trading.marketaggregation.application.port.input.TradeTickHandler;
import com.trading.marketaggregation.event.mapper.TradeTickAvroMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

@Slf4j
@RequiredArgsConstructor
public class TradeTickKafkaConsumer {

    private final TradeTickHandler tradeTickHandler;

    @KafkaListener(topics = "${app.kafka.topics.trades}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, TradeTickEvent> tradeTickEvent) {
        TradeTickEvent avro = tradeTickEvent.value();

        if (avro == null) {
            log.warn("Received null TradeTickEvent: topic={}, partition={}, offset={}, key={}",
                tradeTickEvent.topic(), tradeTickEvent.partition(), tradeTickEvent.offset(), tradeTickEvent.key());
            return;
        }

        TradeTick trade = TradeTickAvroMapper.toDomain(avro);
        tradeTickHandler.handle(trade);
    }
}