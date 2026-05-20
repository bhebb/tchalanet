package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.draw.api.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
import com.tchalanet.server.core.sales.api.command.result.RecordDrawTicketsResultCommand;
import com.tchalanet.server.core.sales.api.command.result.ReconcileTicketsForCorrectedDrawResultCommand;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DrawResultAppliedSalesEventListener")
class DrawResultAppliedSalesEventListenerTest {

    @Test
    @DisplayName("dispatches applied command once per event id")
    void appliedIdempotent() {
        var processed = new HashSet<String>();
        ProcessedEventPort processedEventPort = new ProcessedEventPort() {
            @Override public boolean alreadyProcessed(String handlerKey, UUID eventId) { return processed.contains(handlerKey + ":" + eventId); }
            @Override public void markProcessed(String handlerKey, UUID eventId) { processed.add(handlerKey + ":" + eventId); }
            @Override public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) { return processed.add(handlerKey + ":" + eventId); }
        };

        var dispatched = new ArrayList<Command<?>>();
        CommandBus commandBus = new CommandBus() {
            @Override public <R> R execute(Command<R> command) {
                dispatched.add(command);
                return null;
            }
        };

        var listener = new DrawResultAppliedSalesEventListener(commandBus, processedEventPort);
        var event = new DrawResultAppliedEvent(
            EventId.of(UUID.randomUUID()),
            Instant.now(),
            TenantId.of(UUID.randomUUID()),
            DrawId.of(UUID.randomUUID()),
            LocalDate.now(),
            ResultSlotId.of(UUID.randomUUID()),
            DrawResultId.of(UUID.randomUUID()),
            DrawChannelId.of(UUID.randomUUID())
        );

        listener.onDrawResultApplied(event);
        listener.onDrawResultApplied(event);

        assertThat(dispatched).hasSize(1);
        assertThat(dispatched.getFirst()).isInstanceOf(RecordDrawTicketsResultCommand.class);
    }

    @Test
    @DisplayName("dispatches corrected command once per event id")
    void correctedIdempotent() {
        var processed = new HashSet<String>();
        ProcessedEventPort processedEventPort = new ProcessedEventPort() {
            @Override public boolean alreadyProcessed(String handlerKey, UUID eventId) { return processed.contains(handlerKey + ":" + eventId); }
            @Override public void markProcessed(String handlerKey, UUID eventId) { processed.add(handlerKey + ":" + eventId); }
            @Override public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) { return processed.add(handlerKey + ":" + eventId); }
        };

        var dispatched = new ArrayList<Command<?>>();
        CommandBus commandBus = new CommandBus() {
            @Override public <R> R execute(Command<R> command) {
                dispatched.add(command);
                return null;
            }
        };

        var listener = new DrawResultAppliedSalesEventListener(commandBus, processedEventPort);
        var event = new DrawResultCorrectedEvent(
            EventId.of(UUID.randomUUID()),
            Instant.now(),
            TenantId.of(UUID.randomUUID()),
            DrawId.of(UUID.randomUUID()),
            LocalDate.now(),
            ResultSlotId.of(UUID.randomUUID()),
            DrawResultId.of(UUID.randomUUID()),
            DrawResultId.of(UUID.randomUUID()),
            DrawChannelId.of(UUID.randomUUID()),
            "valid reason"
        );

        listener.onDrawResultCorrected(event);
        listener.onDrawResultCorrected(event);

        assertThat(dispatched).hasSize(1);
        assertThat(dispatched.getFirst()).isInstanceOf(ReconcileTicketsForCorrectedDrawResultCommand.class);
    }
}
