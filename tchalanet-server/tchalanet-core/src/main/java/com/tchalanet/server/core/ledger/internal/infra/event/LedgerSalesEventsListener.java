package com.tchalanet.server.core.ledger.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.ledger.api.command.RecordTicketSaleLedgerCommand;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerSalesEventsListener {

    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TicketPlacedEvent event) {
        log.info(
            "Recording ticket sale ledger entry: tenantId={} ticketId={}",
            event.tenantId(),
            event.ticketId());

        commandBus.execute(
            new RecordTicketSaleLedgerCommand(
                event.tenantId(),
                event.ticketId(),
                event.stakeCents(),
                event.currencyCode(),
                event.occurredAt()));
    }
}
