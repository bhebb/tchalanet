package com.tchalanet.server.core.sales.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.core.payout.internal.domain.event.PayoutPaidEvent;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutPaidCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
class PayoutPaidSalesEventListener {

    private static final String HANDLER_KEY = "sales.payout-paid";

    private final CommandBus commandBus;
    private final ProcessedEventPort processedEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPayoutPaid(PayoutPaidEvent event) {
        if (processedEventPort.alreadyProcessed(HANDLER_KEY, event.eventId().value())) {
            log.info(
                "Duplicate PayoutPaidEvent ignored eventId={} ticketId={} tenantId={}",
                event.eventId(),
                event.ticketId(),
                event.tenantId()
            );
            return;
        }

        log.info(
            "PayoutPaidEvent received eventId={} ticketId={} payoutId={} tenantId={}",
            event.eventId(),
            event.ticketId(),
            event.payoutId(),
            event.tenantId()
        );

        commandBus.execute(new MarkTicketPayoutPaidCommand(
            event.tenantId(),
            event.ticketId(),
            event.payoutId(),
            event.paidBy(),
            event.occurredAt()
        ));

        processedEventPort.markProcessed(HANDLER_KEY, event.eventId().value());
    }
}
