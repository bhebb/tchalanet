package com.tchalanet.server.common.bus.observability;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.bus.QueryBus;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservedQueryBusTest {

    record AllowlistedQry() implements Query<String> {}
    record UnlistedQry() implements Query<String> {}

    @Mock
    QueryBus delegate;

    @Test
    void delegatesAllowlistedQueryAndReturnsResult() {
        when(delegate.ask(any())).thenReturn("data");
        var bus = new ObservedQueryBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedQry"));

        var result = bus.ask(new AllowlistedQry());

        assertThat(result).isEqualTo("data");
        verify(delegate).ask(any(AllowlistedQry.class));
    }

    @Test
    void noSpanCreatedForUnlistedQuery() {
        when(delegate.ask(any())).thenReturn("data");
        var bus = new ObservedQueryBus(delegate, ObservationRegistry.NOOP, Set.of());

        var result = bus.ask(new UnlistedQry());

        assertThat(result).isEqualTo("data");
        verify(delegate).ask(any(UnlistedQry.class));
    }

    @Test
    void queryResultUnchangedWhenTracerAbsent() {
        when(delegate.ask(any())).thenReturn("query-result");
        var bus = new ObservedQueryBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedQry"));

        assertThat(bus.ask(new AllowlistedQry())).isEqualTo("query-result");
    }

    @Test
    void exceptionPropagatesFromAllowlistedQuery() {
        when(delegate.ask(any())).thenThrow(new RuntimeException("query failed"));
        var bus = new ObservedQueryBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedQry"));

        assertThatThrownBy(() -> bus.ask(new AllowlistedQry()))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("query failed");
    }
}
