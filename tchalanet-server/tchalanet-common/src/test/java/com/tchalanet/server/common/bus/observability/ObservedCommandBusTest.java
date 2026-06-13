package com.tchalanet.server.common.bus.observability;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
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
class ObservedCommandBusTest {

    record AllowlistedCmd() implements Command<String> {}
    record UnlistedCmd() implements Command<String> {}

    @Mock
    CommandBus delegate;

    @Test
    void delegatesAllowlistedCommandAndReturnsResult() {
        when(delegate.execute(any())).thenReturn("ok");
        var bus = new ObservedCommandBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedCmd"));

        var result = bus.execute(new AllowlistedCmd());

        assertThat(result).isEqualTo("ok");
        verify(delegate).execute(any(AllowlistedCmd.class));
    }

    @Test
    void noSpanCreatedForUnlistedCommand() {
        when(delegate.execute(any())).thenReturn("ok");
        var bus = new ObservedCommandBus(delegate, ObservationRegistry.NOOP, Set.of());

        var result = bus.execute(new UnlistedCmd());

        assertThat(result).isEqualTo("ok");
        verify(delegate).execute(any(UnlistedCmd.class));
    }

    @Test
    void commandResultUnchangedWhenTracerAbsent() {
        when(delegate.execute(any())).thenReturn("result-value");
        var bus = new ObservedCommandBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedCmd"));

        assertThat(bus.execute(new AllowlistedCmd())).isEqualTo("result-value");
    }

    @Test
    void exceptionPropagatesFromAllowlistedCommand() {
        when(delegate.execute(any())).thenThrow(new IllegalStateException("handler error"));
        var bus = new ObservedCommandBus(delegate, ObservationRegistry.NOOP, Set.of("AllowlistedCmd"));

        assertThatThrownBy(() -> bus.execute(new AllowlistedCmd()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("handler error");
    }
}
