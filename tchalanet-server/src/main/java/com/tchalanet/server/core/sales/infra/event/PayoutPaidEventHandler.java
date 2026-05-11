package com.tchalanet.server.core.sales.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.payout.domain.event.PayoutPaidEvent;
import com.tchalanet.server.core.sales.application.command.model.MarkTicketPayoutPaidCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutPaidSalesEventListener {

    private final CommandBus commandBus;

    @EventListener
    @Transactional
    public void onPayoutPaid(PayoutPaidEvent event) {
        try {
            commandBus.execute(new MarkTicketPayoutPaidCommand(
                event.tenantId(),
                event.ticketId(),
                event.paidBy(),
                "payout_paid",
                event.currency()
            ));
        } catch (Exception ex) {
            log.warn("Failed to mark ticket payout paid. ticketId={}", event.ticketId(), ex);
        }
    }
}
