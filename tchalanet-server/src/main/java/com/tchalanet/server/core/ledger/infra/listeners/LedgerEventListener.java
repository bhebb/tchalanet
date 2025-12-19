package com.tchalanet.server.core.ledger.infra.listeners;

import com.tchalanet.server.core.ledger.application.port.out.LedgerWriterPort;
import com.tchalanet.server.core.ledger.domain.model.LedgerEntry;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventListener {

  private final LedgerWriterPort ledgerWriter;

  @EventListener
  public void onTicketPlaced(TicketPlacedEvent event) {
    try {
      UUID tenantUuid = event.tenantId().value();
      LedgerEntry entry = LedgerEntry.create(
          tenantUuid,
          "TICKET",
          event.ticketId(),
          BigDecimal.valueOf(event.stakeCents()).movePointLeft(2),
          "DEBIT");
      ledgerWriter.append(entry);
    } catch (Exception e) {
      log.error("Failed to append ledger entry for TicketPlacedEvent {}: {}", event.eventId(), e.getMessage(), e);
    }
  }

  @EventListener
  public void onPayoutRegistered(PayoutRegisteredEvent event) {
    try {
      UUID tenantUuid = event.tenantId().value();
      LedgerEntry entry = LedgerEntry.create(
          tenantUuid,
          "PAYOUT",
          event.payoutId(),
          event.amount(),
          "DEBIT");
      ledgerWriter.append(entry);
    } catch (Exception e) {
      log.error("Failed to append ledger entry for PayoutRegisteredEvent {}: {}", event.eventId(), e.getMessage(), e);
    }
  }
}
