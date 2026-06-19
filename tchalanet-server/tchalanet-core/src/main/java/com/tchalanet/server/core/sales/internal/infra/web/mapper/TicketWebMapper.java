package com.tchalanet.server.core.sales.internal.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketOutcome;
import com.tchalanet.server.core.sales.api.command.sell.SellTicketResult;
import com.tchalanet.server.core.sales.api.command.sell.SoldTicketView;
import com.tchalanet.server.core.sales.api.model.print.TicketPrintStateStatus;
import com.tchalanet.server.core.sales.api.model.status.TicketPrintStatus;
import com.tchalanet.server.core.sales.api.model.view.TicketDetailsView;
import com.tchalanet.server.core.sales.api.model.view.TicketRow;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.internal.domain.model.ticket.Ticket;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketResponseOutcome;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketDetailsResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketRowResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketSummaryResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TicketWebMapper {

    public SellTicketResponse toSellResponse(SellTicketResult result) {
        return new SellTicketResponse(
            toWebOutcome(result.outcome()),
            result.ticket() == null ? null : toTicketResponse(result.ticket()),
            result.approvalRequestId()
        );
    }

    private SellTicketResponseOutcome toWebOutcome(SellTicketOutcome outcome) {
        return switch (outcome) {
            case ACCEPTED -> SellTicketResponseOutcome.SOLD;
            case REJECTED -> SellTicketResponseOutcome.REJECTED;
            case PENDING_APPROVAL -> SellTicketResponseOutcome.PENDING_APPROVAL;
        };
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.identity().id(),
            ticket.codes().ticketCode().value(),
            ticket.codes().publicCode().value(),
            ticket.codes().verificationCode().value(),
            ticket.lifecycle().sale().status(),
            ticket.lifecycle().result().status(),
            ticket.lifecycle().settlement().status(),
            ticket.origin().channel(),
            ticket.context().drawId(),
            ticket.context().sellerTerminalId(),
            ticket.money().breakdown().total(),
            ticket.money().potentialPayoutAmount(),
            toPrintStatus(ticket.print().status()),
            ticket.lifecycle().sale().soldAt(),
            ticket.lifecycle().sale().placedAt()
        );
    }

    public TicketResponse toTicketResponse(SoldTicketView ticket) {
        return new TicketResponse(
            ticket.ticketId(),
            ticket.ticketCode(),
            ticket.publicCode(),
            ticket.verificationCode(),
            ticket.saleStatus(),
            ticket.resultStatus(),
            ticket.settlementStatus(),
            ticket.saleChannel(),
            ticket.drawId(),
            ticket.sellerTerminalId(),
            ticket.totalAmount(),
            ticket.potentialPayoutAmount(),
            ticket.printStatus(),
            ticket.soldAt(),
            ticket.placedAt()
        );
    }

    public TicketResponse toTicketResponse(TicketDetailsView view) {
        return new TicketResponse(
            view.id(),
            view.ticketCode(),
            null,
            null,
            view.status(),
            null,
            null,
            null,
            view.drawId(),
            view.sellerTerminalId(),
            toMoney(view.totalAmountCents(), view.currency()),
            null,
            null,
            null,
            view.placedAt()
        );
    }

    public ListTicketsQuery toListTicketsQuery(
        SellerTerminalId sellerTerminalId,
        DrawId drawId,
        String status,
        java.time.Instant from,
        java.time.Instant to,
        TchPageRequest pageReq
    ) {
        return new ListTicketsQuery(sellerTerminalId, drawId, status, from, to, pageReq);
    }

    public TchPage<TicketSummaryResponse> toPagedSummaryResponse(TchPage<TicketRow> page) {
        return TchPageMapper.map(page, this::toSummaryResponse);
    }

    private TicketSummaryResponse toSummaryResponse(TicketRow row) {
        return new TicketSummaryResponse(
            row.id(),
            row.ticketCode(),
            row.status(),
            row.drawId(),
            row.totalAmountCents(),
            row.currency(),
            row.placedAt()
        );
    }

    public TicketDetailsResponse toResponse(TicketDetailsView view) {
        return new TicketDetailsResponse(
            view.id(),
            view.ticketCode(),
            view.status(),
            view.drawId(),
            view.totalAmountCents(),
            view.currency(),
            view.placedAt(),
            view.cancelledAt());
    }

    public TicketRowResponse toResponse(TicketRow row) {
        return new TicketRowResponse(
            row.id(),
            row.ticketCode(),
            row.status(),
            row.drawId(),
            row.totalAmountCents(),
            row.currency(),
            row.placedAt());
    }

    private static TicketPrintStatus toPrintStatus(TicketPrintStateStatus status) {
        if (status == null) {
            return null;
        }
        return TicketPrintStatus.valueOf(status.name());
    }

    private static Money toMoney(long amountCents, String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return null;
        }
        return new Money(BigDecimal.valueOf(amountCents, 2), CurrencyCode.of(currencyCode));
    }
}
