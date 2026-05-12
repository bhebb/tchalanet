package com.tchalanet.server.core.sales.internal.infra.event;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.core.ledger.application.command.model.RecordTicketSaleLedgerCommand;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesLedgerListener {

  static final String CONSUMER = "ledger.record_ticket_sale";

  private final CommandBus commandBus;
  private final ProcessedEventPort processedEvent;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onTicketPlaced(TicketPlacedEvent event) {
    if (processedEvent.alreadyProcessed(CONSUMER, event.eventId().value())) {
      log.debug("Ledger event already processed: eventId={}", event.eventId());
      return;
    }
    commandBus.execute(
        new RecordTicketSaleLedgerCommand(
            event.tenantId(), event.ticketId(), event.stakeCents(), event.occurredAt()));
    processedEvent.markProcessedIfAbsent(CONSUMER, event.eventId().value());
  }
}
