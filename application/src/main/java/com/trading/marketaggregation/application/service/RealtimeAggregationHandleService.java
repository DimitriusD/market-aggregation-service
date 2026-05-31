package com.trading.marketaggregation.application.service;

import com.trading.marketaggregation.application.domain.model.AggregationDecision;
import com.trading.marketaggregation.application.domain.model.AggregationResult;
import com.trading.marketaggregation.application.domain.model.MarketBar;
import com.trading.marketaggregation.application.domain.model.TradeTick;
import com.trading.marketaggregation.application.domain.service.RealtimeOneMinuteBarAggregator;
import com.trading.marketaggregation.application.port.input.TradeTickHandler;
import com.trading.marketaggregation.application.port.output.MarketBarPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;

public final class RealtimeAggregationHandleService implements TradeTickHandler {

    private static final Logger log = LoggerFactory.getLogger(RealtimeAggregationHandleService.class);

    private final RealtimeOneMinuteBarAggregator aggregator;
    private final MarketBarPublisherPort publisher;
    private final Clock clock;

    public RealtimeAggregationHandleService(RealtimeOneMinuteBarAggregator aggregator,
                                            MarketBarPublisherPort publisher,
                                            Clock clock) {
        this.aggregator = aggregator;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public void handle(TradeTick trade) {
        long nowMs = clock.millis();
        AggregationResult result = aggregator.onTrade(trade, nowMs);

        if (result.decision() == AggregationDecision.LATE_TRADE_IGNORED) {
            log.warn("Late trade ignored: instrumentId={}, exchangeTs={}",
                instrumentIdOf(trade), exchangeTsOf(trade));
        }

        if (result.decision() == AggregationDecision.INVALID_TRADE_SKIPPED) {
            log.warn("Invalid trade skipped: instrumentId={}, price={}, quantity={}",
                instrumentIdOf(trade), trade != null ? trade.price() : null,
                trade != null ? trade.quantity() : null);
        }

        for (MarketBar bar : result.barsToPublish()) {
            publisher.publish(bar);
            if (bar.closed()) {
                log.info("Published closed bar: instrumentId={}, openTime={}, O={} H={} L={} C={}, trades={}",
                    bar.metadata().instrumentId(), bar.openTime(),
                    bar.open(), bar.high(), bar.low(), bar.close(), bar.tradeCount());
            } else {
                log.debug("Published partial bar: instrumentId={}, openTime={}, close={}, trades={}",
                    bar.metadata().instrumentId(), bar.openTime(), bar.close(), bar.tradeCount());
            }
        }
    }

    private String instrumentIdOf(TradeTick trade) {
        return trade != null && trade.metadata() != null ? trade.metadata().instrumentId() : null;
    }

    private Long exchangeTsOf(TradeTick trade) {
        return trade != null && trade.metadata() != null ? trade.metadata().exchangeTs() : null;
    }
}