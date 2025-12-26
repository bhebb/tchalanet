package com.tchalanet.server.core.sales.infra.event;

import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromSalesPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesLedgerListener {

    private final RecordLedgerFromSalesPort ledgerPort;

    @EventListener
    public void onTicketPlaced(TicketPlacedEvent event) {
        try {
            ledgerPort.recordTicketSale(
                event.tenantId(),
                event.ticketId(),
                event.stakeCents(),
                event.occurredAt()
            );
        } catch (Exception e) {
            log.error(
                "Ledger recording failed for TicketPlacedEvent eventId={} tenantId={} ticketId={}",
                event.eventId(),
                event.tenantId().value(),
                event.ticketId(),
                e
            );
        }
    }
}
