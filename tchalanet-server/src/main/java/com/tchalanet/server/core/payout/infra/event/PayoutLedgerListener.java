package com.tchalanet.server.core.payout.infra.event;

import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromPayoutPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutLedgerListener {

  private final RecordLedgerFromPayoutPort ledgerPort;

  @TransactionalEventListener(phase = AFTER_COMMIT)
  public void onPayoutRegistered(PayoutRegisteredEvent event) {
    try {
      ledgerPort.recordPayout(
          event.tenantId(), event.payoutId(), event.amount(), event.occurredAt());
    } catch (Exception e) {
      log.error(
          "Ledger recording failed for PayoutRegisteredEvent eventId={} tenantId={} payoutId={}",
          event.eventId().value(),
          event.tenantId().value(),
          event.payoutId(),
          e);
    }
  }
}
