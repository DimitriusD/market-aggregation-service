package com.trading.marketaggregation.config;

import com.trading.marketaggregation.application.domain.service.BarBucketCalculator;
import com.trading.marketaggregation.application.domain.service.BarPublishPolicy;
import com.trading.marketaggregation.application.domain.service.RealtimeOneMinuteBarAggregator;
import com.trading.marketaggregation.application.port.input.TradeTickHandler;
import com.trading.marketaggregation.application.port.output.MarketBarPublisherPort;
import com.trading.marketaggregation.application.service.RealtimeAggregationHandleService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AggregationProperties.class)
public class AggregationConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    BarBucketCalculator barBucketCalculator() {
        return new BarBucketCalculator();
    }

    @Bean
    BarPublishPolicy barPublishPolicy(AggregationProperties properties) {
        return new BarPublishPolicy(properties.partialPublishIntervalMs());
    }

    @Bean
    RealtimeOneMinuteBarAggregator realtimeOneMinuteBarAggregator(BarBucketCalculator bucketCalculator,
                                                                   BarPublishPolicy publishPolicy) {
        return new RealtimeOneMinuteBarAggregator(bucketCalculator, publishPolicy);
    }

    @Bean
    TradeTickHandler tradeTickHandler(RealtimeOneMinuteBarAggregator aggregator,
                                     MarketBarPublisherPort publisher,
                                     Clock clock) {
        return new RealtimeAggregationHandleService(aggregator, publisher, clock);
    }
}
