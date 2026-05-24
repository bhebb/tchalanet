package com.tchalanet.server.core.promotion.internal.infra.event;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.core.promotion.api.command.applied.CreateAppliedPromotionSnapshotCommand;
import com.tchalanet.server.core.promotion.api.model.PromotionDecision;
import com.tchalanet.server.core.sales.api.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event-driven option.
 * Enable only if ArchUnit accepts promotionDecision -> sales.api.event dependency.
 * Otherwise keep fallback: sales calls CreateAppliedPromotionSnapshotCommand in AfterCommit.
 */
@Component
@RequiredArgsConstructor
class TicketPlacedPromotionSnapshotListener {
    private final CommandBus commandBus;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TicketPlacedEvent event) {
        if (event.saleStatus() != TicketSaleStatus.APPROVED) {
            return; // PENDING_APPROVAL should be handled on TicketApprovedEvent later.
        }
        PromotionDecision decision = event.promotionDecision();
        if (decision == null || !decision.applied()) {
            return;
        }
        commandBus.execute(new CreateAppliedPromotionSnapshotCommand(
            event.tenantId(),
            event.ticketId(),
            decision
        ));
    }
}
