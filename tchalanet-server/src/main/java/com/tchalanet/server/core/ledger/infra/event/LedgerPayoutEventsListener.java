package com.tchalanet.server.core.ledger.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.ledger.application.command.model.RecordPayoutPaidLedgerCommand;
import com.tchalanet.server.core.payout.domain.event.PayoutPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerPayoutEventsListener {

    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PayoutPaidEvent event) {
        log.info(
            "Recording payout ledger entry: tenantId={} payoutId={}",
            event.tenantId(),
            event.payoutId());

        commandBus.execute(
            new RecordPayoutPaidLedgerCommand(
                event.tenantId(),
                event.payoutId(),
                event.amountCents(),
                event.currency(),
                event.occurredAt()));
    }
}
