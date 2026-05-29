package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.core.payout.internal.domain.event.PayoutReversedEvent;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutReversedCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
class PayoutReversedSalesEventListener {

    private static final String HANDLER_KEY = "sales.payout-reversed";

    private final CommandBus commandBus;
    private final ProcessedEventPort processedEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPayoutReversed(PayoutReversedEvent event) {
        if (processedEventPort.alreadyProcessed(HANDLER_KEY, event.eventId().value())) {
            log.info(
                "Duplicate PayoutReversedEvent ignored eventId={} ticketId={} tenantId={}",
                event.eventId(), event.ticketId(), event.tenantId());
            return;
        }

        log.info(
            "PayoutReversedEvent received eventId={} ticketId={} payoutId={} tenantId={}",
            event.eventId(), event.ticketId(), event.payoutId(), event.tenantId());

        commandBus.execute(new MarkTicketPayoutReversedCommand(
            event.tenantId(),
            event.ticketId(),
            event.payoutId(),
            event.reversedBy(),
            event.occurredAt()
        ));

        processedEventPort.markProcessed(HANDLER_KEY, event.eventId().value());
    }
}
