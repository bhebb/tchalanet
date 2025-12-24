package com.tchalanet.server.core.payout.infra.event;

import com.tchalanet.server.core.ledger.application.port.in.RecordLedgerFromPayoutPort;
import com.tchalanet.server.core.payout.domain.event.PayoutRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutLedgerListener {

    private final RecordLedgerFromPayoutPort ledgerPort;

    @EventListener
    public void onPayoutRegistered(PayoutRegisteredEvent event) {
        try {
            ledgerPort.recordPayout(
                event.tenantId().value(),
                event.payoutId(),
                event.amount(),
                event.occurredAt()
            );
        } catch (Exception e) {
            log.error(
                "Ledger recording failed for PayoutRegisteredEvent eventId={} tenantId={} payoutId={}",
                event.eventId(),
                event.tenantId().value(),
                event.payoutId(),
                e
            );
        }
    }
}
