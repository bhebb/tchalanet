package com.tchalanet.server.core.sales.internal.infra.web.mapper;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.sales.api.model.TicketResultStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.api.command.CancelSaleCommand;
import com.tchalanet.server.core.sales.api.command.OverrideTicketResultCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SoldTicketView;
import com.tchalanet.server.core.sales.api.model.TicketStatus;
import com.tchalanet.server.core.sales.api.query.ListTicketsQuery;
import com.tchalanet.server.core.sales.api.query.TicketDetailsView;
import com.tchalanet.server.core.sales.api.query.TicketSummaryView;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.infra.web.model.CancelSaleResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.CancelTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.OverrideTicketResultRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketSummaryResponse;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketWebMapper {

    private final TchContextResolver contextResolver;

    public SellTicketCommand toSellCommand(SellTicketRequest request) {
        // TODO(sales-refactor): rebuild SellTicketRequest->SellTicketCommand mapping after request contract alignment.
        throw new UnsupportedOperationException("TODO: sales command mapping pending refactor");
    }

    public SellTicketCommand toSellCommand(TchRequestContext ctx, SellTicketRequest request) {
        return toSellCommand(request);
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
            String.valueOf(ticket.id()),
            ticket.publicCode(),
            String.valueOf(ticket.saleStatus()),
            String.valueOf(ticket.saleOrigin()),
            String.valueOf(ticket.syncStatus()),
            ticket.money().stakeAmount(),
            ticket.money().feeAmount(),
            ticket.money().totalAmount()
        );
    }

    public TicketResponse toTicketResponse(TicketDetailsView t) {
        return new TicketResponse(
            String.valueOf(t.id()),
            t.publicCode(),
            String.valueOf(t.saleStatus()),
            null,
            null,
            t.totalAmount(),
            null,
            t.totalAmount()
        );
    }

    public TicketResponse toTicketResponse(SoldTicketView t) {
        return new TicketResponse(
            String.valueOf(t.ticketId()),
            t.publicCode(),
            String.valueOf(t.saleStatus()),
            null,
            null,
            t.totalAmount(),
            null,
            t.totalAmount()
        );
    }

    public CancelSaleCommand toCancelSaleCommand(TicketId ticketId, CancelTicketRequest req) {
        var ctx = Objects.requireNonNull(contextResolver.currentOrNull(), "Missing request context");
        var performedBy = UserId.of(ctx.userUuid());

        return new CancelSaleCommand(
            ctx.tenantIdSafe(),
            ticketId,
            performedBy,
            req.reason(),
            ctx.tenantCurrency() == null ? null : ctx.tenantCurrency().getCurrencyCode());
    }

    public CancelSaleResponse toCancelSaleResponse(Object result) {
        if (result instanceof Ticket ticket) {
            return new CancelSaleResponse(toTicketResponse(ticket), "CANCELLED", java.util.List.of());
        }
        return new CancelSaleResponse(null, "CANCELLED", java.util.List.of());
    }

    public OverrideTicketResultCommand toOverrideTicketResultCommand(
        TicketId ticketId,
        UserId userId,
        OverrideTicketResultRequest request
    ) {
        var ctx = Objects.requireNonNull(contextResolver.currentOrNull(), "Missing request context");
        var performedBy = ctx.currentUserIdRequired();

        return new OverrideTicketResultCommand(
            ticketId,
            request.totalPayout(),
            new TicketStatus(
                null,
                request.status() == null ? null : request.status().resultStatus(),
                null
            ),
            request.reason(),
            performedBy,
            null
        );
    }

    public ListTicketsQuery toListTicketsQuery(
        TerminalId terminalId,
        DrawId drawId,
        String status,
        Instant from,
        Instant to,
        TchPageRequest pageReq
    ) {
        TicketResultStatus ticketResultStatus = parseResultStatus(status);

        var filter = new ListTicketsQuery.TicketFilter(
            null,
            terminalId,
            drawId,
            ticketResultStatus,
            from,
            to
        );

        return new ListTicketsQuery(filter, pageReq.pageable());
    }

    public TchPage<TicketSummaryResponse> toPagedSummaryResponse(TchPage<TicketSummaryView> page) {
        var items = page.items().stream()
            .map(dto -> new TicketSummaryResponse(
                dto.id().value(),
                dto.ticketCode(),
                dto.publicCode(),
                dto.status(),
                dto.totalAmount(),
                dto.createdAt(),
                dto.terminalLabel(),
                dto.drawInfo()
            ))
            .toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    private TicketResultStatus parseResultStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return TicketResultStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ProblemRest.badRequest("Invalid status filter value: '" + raw + "'");
        }
    }
}
