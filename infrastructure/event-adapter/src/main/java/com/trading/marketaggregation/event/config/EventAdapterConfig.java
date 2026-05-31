package com.trading.marketaggregation.event.config;

import com.trading.contracts.market.MarketBarEvent;
import com.trading.marketaggregation.application.port.input.TradeTickHandler;
import com.trading.marketaggregation.event.consumer.TradeTickKafkaConsumer;
import com.trading.marketaggregation.event.mapper.AvroMappingException;
import com.trading.marketaggregation.event.publisher.MarketBarKafkaPublisher;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EventAdapterConfig {

    @Bean
    ProducerFactory<String, MarketBarEvent> marketBarProducerFactory(
        @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
        @Value("${spring.kafka.producer.properties.schema.registry.url:http://localhost:8081}") String schemaRegistryUrl) {

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put("schema.registry.url", schemaRegistryUrl);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    KafkaTemplate<String, MarketBarEvent> marketBarKafkaTemplate(
        ProducerFactory<String, MarketBarEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    MarketBarKafkaPublisher marketBarKafkaPublisher(
        KafkaTemplate<String, MarketBarEvent> kafkaTemplate,
        @Value("${app.kafka.topics.bars}") String topic) {
        return new MarketBarKafkaPublisher(kafkaTemplate, topic);
    }

    @Bean
    KafkaTemplate<Object, Object> dltKafkaTemplate(
        @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers,
        @Value("${spring.kafka.producer.properties.schema.registry.url:http://localhost:8081}") String schemaRegistryUrl) {

        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put("schema.registry.url", schemaRegistryUrl);

        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(config));
    }

    @Bean
    CommonErrorHandler consumerErrorHandler(KafkaTemplate<Object, Object> dltKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1_000L, 3));
        handler.addNotRetryableExceptions(AvroMappingException.class);

        return handler;
    }

    @Bean
    TradeTickKafkaConsumer tradeTickKafkaConsumer(TradeTickHandler tradeTickHandler) {
        return new TradeTickKafkaConsumer(tradeTickHandler);
    }
}