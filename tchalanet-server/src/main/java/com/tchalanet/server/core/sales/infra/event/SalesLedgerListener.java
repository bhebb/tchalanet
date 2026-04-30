package com.tchalanet.server.core.sales.infra.event;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.ledger.application.command.model.RecordTicketSaleLedgerCommand;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SalesLedgerListener {

  private final CommandBus commandBus;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onTicketPlaced(TicketPlacedEvent event) {
    commandBus.send(
        new RecordTicketSaleLedgerCommand(
            event.tenantId(), event.ticketId(), event.stakeCents(), event.occurredAt()));
  }
}
