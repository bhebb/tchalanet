package com.tchalanet.server.core.sales.infra.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.types.enums.BetType;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.ledger.application.command.model.RecordTicketSaleLedgerCommand;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SalesLedgerListenerTest {

  @Test
  void skipsAlreadyProcessedEvents() {
    var event = event();
    var calls = new ArrayList<String>();
    var commandBus = new RecordingCommandBus(calls);
    var processedEvent = new RecordingProcessedEventPort(calls);
    processedEvent.alreadyProcessed = true;

    new SalesLedgerListener(commandBus, processedEvent).onTicketPlaced(event);

    assertThat(commandBus.sent).isNull();
    assertThat(processedEvent.markCalls).isZero();
    assertThat(calls).containsExactly("alreadyProcessed");
  }

  @Test
  void doesNotMarkProcessedWhenLedgerCommandFails() {
    var event = event();
    var failure = new IllegalStateException("ledger down");
    var calls = new ArrayList<String>();
    var commandBus = new RecordingCommandBus(calls);
    var processedEvent = new RecordingProcessedEventPort(calls);
    commandBus.failure = failure;

    var listener = new SalesLedgerListener(commandBus, processedEvent);

    assertThatThrownBy(() -> listener.onTicketPlaced(event)).isSameAs(failure);
    assertThat(processedEvent.markCalls).isZero();
    assertThat(calls).containsExactly("alreadyProcessed", "send");
  }

  @Test
  void sendsLedgerCommandBeforeMarkingProcessed() {
    var event = event();
    var calls = new ArrayList<String>();
    var commandBus = new RecordingCommandBus(calls);
    var processedEvent = new RecordingProcessedEventPort(calls);

    new SalesLedgerListener(commandBus, processedEvent).onTicketPlaced(event);

    assertThat(calls).containsExactly("alreadyProcessed", "send", "markProcessedIfAbsent");
    assertThat(processedEvent.markCalls).isEqualTo(1);
    assertThat(commandBus.sent).isInstanceOf(RecordTicketSaleLedgerCommand.class);
    var command = (RecordTicketSaleLedgerCommand) commandBus.sent;
    assertThat(command.tenantId()).isEqualTo(event.tenantId());
    assertThat(command.ticketId()).isEqualTo(event.ticketId());
    assertThat(command.stakeCents()).isEqualTo(event.stakeCents());
    assertThat(command.occurredAt()).isEqualTo(event.occurredAt());
  }

  private static TicketPlacedEvent event() {
    return new TicketPlacedEvent(
        EventId.of(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        Instant.parse("2026-05-06T12:00:00Z"),
        TenantId.of(UUID.fromString("00000000-0000-0000-0000-000000000002")),
        TicketId.of(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        OutletId.of(UUID.fromString("00000000-0000-0000-0000-000000000004")),
        null,
        null,
        null,
        DrawId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")),
        null,
        "CASH3",
        500,
        "USD",
        List.of(new TicketPlacedEvent.Line(BetType.MATCH_1_2D, "12", 500, 4000, null)));
  }

  private static final class RecordingCommandBus implements CommandBus {
    private final List<String> calls;
    private Command<?> sent;
    private RuntimeException failure;

    private RecordingCommandBus(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public <R> R execute(Command<R> command) {
      calls.add("send");
      sent = command;
      if (failure != null) {
        throw failure;
      }
      return null;
    }
  }

  private static final class RecordingProcessedEventPort implements ProcessedEventPort {
    private final List<String> calls;
    private boolean alreadyProcessed;
    private int markCalls;

    private RecordingProcessedEventPort(List<String> calls) {
      this.calls = calls;
    }

    @Override
    public boolean alreadyProcessed(String handlerKey, UUID eventId) {
      calls.add("alreadyProcessed");
      return alreadyProcessed;
    }

    @Override
    public void markProcessed(String handlerKey, UUID eventId) {
      markProcessedIfAbsent(handlerKey, eventId);
    }

    @Override
    public boolean markProcessedIfAbsent(String handlerKey, UUID eventId) {
      calls.add("markProcessedIfAbsent");
      markCalls++;
      return true;
    }
  }
}
