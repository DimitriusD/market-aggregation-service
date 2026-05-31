package com.trading.marketaggregation.application.domain.service;

import com.trading.marketaggregation.application.domain.model.Metadata;
import com.trading.marketaggregation.application.domain.model.OneMinuteBarState;
import com.trading.marketaggregation.application.domain.model.TradeTick;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarPublishPolicyTest {

    @Test
    void shouldPublishPartialFirstTime() {
        BarPublishPolicy policy = new BarPublishPolicy(1000);
        OneMinuteBarState state = createState();

        assertThat(policy.shouldPublishPartial(state, 100)).isTrue();
    }

    @Test
    void shouldNotPublishPartialBeforeInterval() {
        BarPublishPolicy policy = new BarPublishPolicy(1000);
        OneMinuteBarState state = createState();
        state.markPartialPublished(100);

        assertThat(policy.shouldPublishPartial(state, 500)).isFalse();
        assertThat(policy.shouldPublishPartial(state, 1099)).isFalse();
    }

    @Test
    void shouldPublishPartialAfterInterval() {
        BarPublishPolicy policy = new BarPublishPolicy(1000);
        OneMinuteBarState state = createState();
        state.markPartialPublished(100);

        assertThat(policy.shouldPublishPartial(state, 1100)).isTrue();
    }

    @Test
    void shouldAlwaysPublishClosed() {
        BarPublishPolicy policy = new BarPublishPolicy(1000);
        assertThat(policy.shouldPublishClosed()).isTrue();
    }

    @Test
    void invalidIntervalThrows() {
        assertThatThrownBy(() -> new BarPublishPolicy(0))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BarPublishPolicy(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private OneMinuteBarState createState() {
        long exchangeTs = 1_000_000L;
        Metadata metadata = new Metadata(
            1, "TRADE_TICK", "BINANCE", "SPOT", "BTC", "USDT", "BTCUSDT",
            "BINANCE:SPOT:BTCUSDT", "event-1", "canonical.market.trade.v1",
            exchangeTs, exchangeTs, exchangeTs
        );
        TradeTick trade = new TradeTick(metadata, null, new BigDecimal("100"), new BigDecimal("1"), null);
        return OneMinuteBarState.start(trade, 960_000L);
    }
}