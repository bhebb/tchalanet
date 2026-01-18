package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.BreachOutcome;
import com.tchalanet.server.common.web.advice.ApiResponseContext;
import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.NoticeSeverity;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketOutcome;
import com.tchalanet.server.core.sales.application.command.model.SellTicketResult;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketPlacedEvent;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.service.TicketSaleFactory;
import com.tchalanet.server.core.sales.domain.service.TicketSalePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SellTicketCommandHandler implements CommandHandler<SellTicketCommand, SellTicketResult> {

    private final TicketSalePolicy salePolicy;
    private final TicketSaleFactory ticketFactory;
    private final TicketWritterPort ticketWriter;
    private final DomainEventPublisher publisher;

    @Override
    @TchTx
    public SellTicketResult handle(SellTicketCommand command) {

        var prepared = salePolicy.prepareSale(command);

        // Approval required => persist PENDING_APPROVAL ticket (not null)
        if (prepared.limits().overallOutcome() == BreachOutcome.BLOCK) {
            Ticket pending =
                ticketFactory.newPendingApprovalTicket(
                    command.tenantId(),
                    command.terminalId(),
                    prepared.session(),
                    prepared.draw(),
                    prepared.ticketLines(),
                    prepared.now());

            var saved = ticketWriter.save(pending);

            var approvalRequestId = UUID.randomUUID(); // TODO integrate approval domain later
            ApiResponseContext.get()
                .addNotice(
                    new ApiNotice(
                        "APPROVAL_REQUIRED",
                        "Transaction requires approval",
                        "sales",
                        NoticeSeverity.WARN,
                        Map.of("approvalRequestId", approvalRequestId)));

            return new SellTicketResult(saved, SellTicketOutcome.PENDING_APPROVAL, approvalRequestId);
        }

        var sold =
            ticketFactory.newSoldTicket(
                command.tenantId(),
                command.terminalId(),
                prepared.session(),
                prepared.draw(),
                prepared.ticketLines(),
                prepared.now());

        var saved = ticketWriter.save(sold);

        // publish event
        var placed =
            new TicketPlacedEvent(
                UUID.randomUUID(),
                prepared.now(),
                saved.getTenantId(),
                saved.getId(),
                prepared.session().outletId(),
                prepared.session().userId().uuid(),
                prepared.session().id(),
                saved.getDrawId(),
                prepared.ticketLines().isEmpty() ? "" : prepared.ticketLines().get(0).gameCode(),
                saved.getTotalAmount().movePointRight(2).longValue(),
                command.currency());

        AfterCommit.run(() -> publisher.publish(placed));

        var outcome =
            prepared.limits().overallOutcome() == com.tchalanet.server.common.types.enums.BreachOutcome.WARN
                ? SellTicketOutcome.SUCCESS_WITH_WARNINGS
                : SellTicketOutcome.SUCCESS;

        log.info("Ticket sold ticketId={} status={}", saved.getId(), saved.getSaleStatus());
        return new SellTicketResult(saved, outcome, null);
    }
}
