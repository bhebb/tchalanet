package com.tchalanet.server.core.draw.infra.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.core.draw.application.command.model.ApplyExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.domain.event.DrawResultIngestedEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DrawDomainEventListenerTest {

  @Test
  void onDrawResultIngestedTriggersApply() {
    var processed = new InMemoryProcessedEventPort();
    var commandBus = new RecordingCommandBus();
    var listener = new DrawEventListener(processed, null, commandBus);
    var event = event(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    listener.onDrawResultIngested(event);

    assertThat(commandBus.applyCommandSent).isTrue();
  }

  private static DrawResultIngestedEvent event(UUID eventId) {
    return new DrawResultIngestedEvent(
        EventId.of(eventId),
        Instant.parse("2026-04-28T12:00:00Z"),
        null, // tenantId always null for global ingestion
        ResultSlotId.of(UUID.fromString("00000000-0000-0000-0000-000000000104")),
        "HT_MIDI", // resultSlotKey
        DrawResultId.of(UUID.fromString("00000000-0000-0000-0000-000000000105")),
        Instant.parse("2026-04-28T11:55:00Z"),
        LocalDate.parse("2026-04-28"));
  }

  private static class InMemoryProcessedEventPort implements ProcessedEventPort {
    private final Set<UUID> processed = new HashSet<>();
    int markProcessedCalls;

    @Override
    public boolean alreadyProcessed(String handlerKey, UUID eventId) {
      return processed.contains(eventId);
    }

    @Override
    public void markProcessed(String handlerKey, UUID eventId) {
      markProcessedIfAbsent(handlerKey, eventId);
    }

    @Override
    public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) {
      if (processed.contains(eventId)) {
        return false;
      }
      processed.add(eventId);
      markProcessedCalls++;
      return true;
    }
  }

  private static class RecordingCommandBus implements CommandBus {
    boolean applyCommandSent = false;

    @Override
    public <R> R send(com.tchalanet.server.common.bus.Command<R> command) {
      if (command instanceof ApplyExternalResultsWindowCommand) {
        applyCommandSent = true;
      }
      return null;
    }
  }
}
