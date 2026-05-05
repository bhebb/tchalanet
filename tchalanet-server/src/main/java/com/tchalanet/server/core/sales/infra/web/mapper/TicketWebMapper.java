package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.common.context.TchContextResolver;
import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.sales.application.command.model.CancelSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.OverrideTicketResultCommand;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.model.TicketStatus;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.application.query.model.TicketDetailsView;
import com.tchalanet.server.core.sales.application.query.model.TicketSummaryView;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.web.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketWebMapper {

    private final TchContextResolver contextResolver;

    public SellTicketCommand toSellCommand(SellTicketRequest request) {
        var ctx = Objects.requireNonNull(
            contextResolver.currentOrNull(),
            "Missing request context (tenant/user)"
        );

        TenantId tenantId = ctx.tenantIdSafe();
        UserId cashierId = ctx.currentUserIdRequired();

        return new SellTicketCommand(
            tenantId,
            TerminalId.of(request.terminalId()),
            cashierId,
            DrawId.of(request.drawId()),
            request.lines().stream()
                .map(l -> new SellTicketCommand.LineCommand(
                    l.gameCode(),     // GameCode enum attendu
                    l.selection(),
                    l.stake(),
                    l.betType(),
                    l.betOption()
                ))
                .collect(Collectors.toList()),
            request.currency()
        );
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getTenantId(),
            ticket.getTerminalId(),
            ticket.getDrawId(),
            ticket.getTicketCode(),
            ticket.getPublicCode(),

            ticket.getSaleStatus(),
            ticket.getResultStatus(),
            ticket.getSettlementStatus(),

            ticket.getTotalAmount(),
            ticket.getWinningAmount(),
            ticket.getResultedAt(),

            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getLines().stream().map(this::toLineResponse).toList()
        );
    }

    // overload to accept TicketDetailsView
    public TicketResponse toTicketResponse(TicketDetailsView t) {
        return new TicketResponse(
            t.id(),
            t.tenantId(),
            t.terminalId(),
            t.drawId(),
            t.ticketCode(),
            t.publicCode(),

            t.saleStatus(),
            t.resultStatus(),
            t.settlementStatus(),

            t.totalAmount(),
            t.winningAmount(),
            t.resultedAt(),

            t.createdAt(),
            t.updatedAt(),
            t.lines().stream()
                .map(l -> new TicketResponse.LineResponse(
                    l.gameCode().name(),
                    l.betType(),
                    l.betOption() == null ? null : Integer.valueOf(l.betOption()),
                    l.selection(),
                    l.stake(),
                    l.oddsSnapshot(),
                    l.potentialPayout()
                ))
                .collect(Collectors.toList())
        );
    }

    public CancelSaleCommand toCancelSaleCommand(
        com.tchalanet.server.common.types.id.TicketId ticketId,
        CancelTicketRequest req
    ) {
        // NOTE: performedBy devrait venir du ctx (RLS), pas du body.
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
        com.tchalanet.server.common.types.id.TicketId ticketId,
        OverrideTicketResultRequest request
    ) {
        var ctx = Objects.requireNonNull(contextResolver.currentOrNull(), "Missing request context");
        // performedBy always from context — never trust body for sensitive audited actions
        var performedBy = ctx.currentUserIdRequired();

        return new OverrideTicketResultCommand(
            ticketId,
            request.totalPayout(),
            new TicketStatus(
                null, // saleStatus not overridable
                request.status() == null ? null : request.status().resultStatus(),
                null  // settlementStatus not overridable
            ),
            request.reason(),
            performedBy,
            null // performedAt from Clock/handler, not body
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
            null, // tenantId derived from RLS/context
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

    private TicketResponse.LineResponse toLineResponse(TicketLine line) {
        Integer opt = line.betOption() == null ? null : Integer.valueOf(line.betOption());
        return new TicketResponse.LineResponse(
            line.gameCode().name(),
            line.betType(),
            opt,
            line.selection(),
            line.stake(),
            line.oddsSnapshot(),
            line.potentialPayout()
        );
    }

    private TicketResultStatus parseResultStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return TicketResultStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid status filter value: '" + raw + "'");
        }
    }
}
