package com.tchalanet.server.core.payout.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.payout.api.command.OpenPayoutClaimFromSettlementCommand;
import com.tchalanet.server.core.sales.api.event.TicketWinningSettlementCreatedEvent;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketResultedPayoutEventListener {

    private static final String HANDLER_KEY = "payout.winning-settlement-created";

    private final CommandBus commandBus;
    private final ProcessedEventPort processedEventPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketWinningSettlementCreated(TicketWinningSettlementCreatedEvent event) {
        if (processedEventPort.alreadyProcessed(HANDLER_KEY, event.eventId().value())) {
            log.info(
                "payout.winning-settlement.duplicate ticketId={} eventId={}",
                event.ticketId(), event.eventId());
            return;
        }

        log.info(
            "payout.winning-settlement.received ticketId={} drawId={} amountCents={}",
            event.ticketId(), event.drawId(), event.amountCents());

        commandBus.execute(new OpenPayoutClaimFromSettlementCommand(
            event.eventId(),
            event.tenantId(),
            event.ticketId(),
            event.drawId(),
            event.amountCents(),
            event.currency(),
            event.sellingOutletId(),
            event.sellingSessionId()));

        processedEventPort.markProcessed(HANDLER_KEY, event.eventId().value());
    }
}
