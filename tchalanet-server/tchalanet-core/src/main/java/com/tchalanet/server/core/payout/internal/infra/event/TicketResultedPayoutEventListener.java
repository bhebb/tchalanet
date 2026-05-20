package com.tchalanet.server.core.payout.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutCommand;
import com.tchalanet.server.core.sales.api.event.TicketResultedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketResultedPayoutEventListener {

    private final CommandBus commandBus;

    @EventListener
    public void onTicketResulted(TicketResultedEvent event) {

        if (event.resultStatus() != TicketResultStatus.WON) {
            return;
        }

        if (event.totalPayout() == null || event.totalPayout().signum() <= 0) {
            return;
        }

        try {
            commandBus.execute(
                new RegisterPayoutCommand(
                    event.tenantId(),
                    event.ticketId(),
                    null, // payingOutletId
                    null, // payingSessionId
                    null, // id
                    null, // requestedBy (system)
                    "ticket_resulted"));

            log.info(
                "Payout registration triggered for winning ticket ticketId={} tenantId={} payout={}",
                event.ticketId(),
                event.tenantId(),
                event.totalPayout());

        } catch (Exception ex) {

            // idempotency / duplicate protection:
            // RegisterPayoutCommandHandler already validates existing payout

            log.warn(
                "Failed to register payout for ticket ticketId={} tenantId={} reason={}",
                event.ticketId(),
                event.tenantId(),
                ex.getMessage());
        }
    }
}
