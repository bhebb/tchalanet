package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.TicketResponse;
import com.tchalanet.server.core.sales.infra.web.model.TicketStatus;
import com.tchalanet.server.core.sales.infra.web.model.TicketSummaryResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.stream.Collectors;

@Component
public class TicketWebMapper {

    public SellTicketCommand toSellCommand(SellTicketRequest request) {
        return new SellTicketCommand(
            TenantId.of(request.tenantId()),
            TerminalId.of(request.terminalId()),
            UserId.of(request.cashierId()),
            DrawId.of(request.drawId()),
            request.lines().stream()
                .map(
                    l ->
                        new SellTicketCommand.LineCommand(
                            l.gameCode(),
                            l.selection(),
                            l.stake(),
                            l.betType(),
                            l.betOption() // ✅ new
                        ))
                .collect(Collectors.toList()),
            request.currency());
    }

    public TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.getId(),
            ticket.getTenantId(),
            ticket.getTerminalId(),
            ticket.getDrawId(),
            ticket.getTicketCode(),
            ticket.getPublicCode(),

            // ✅ decoupled statuses
            ticket.getSaleStatus(),
            ticket.getResultStatus(),
            ticket.getSettlementStatus(),

            ticket.getTotalAmount(),
            ticket.getWinningAmount(),
            ticket.getResultedAt(),

            ticket.getCreatedAt(),
            ticket.getUpdatedAt(),
            ticket.getLines().stream().map(this::toLineResponse).toList());
    }

    /**
     * Build ListTicketsQuery from controller parameters.
     */
    public ListTicketsQuery toListTicketsQuery(
        TerminalId terminalId,
        DrawId drawId,
        String status,
        Instant from,
        Instant to,
        int page,
        int size) {

        // Parse status if provided - for now we don't filter by status
        // TODO: Implement status filter parsing when needed
        com.tchalanet.server.core.sales.infra.web.model.TicketStatus ticketStatus = null;

        var filter = new ListTicketsQuery.TicketFilter(
            null, // tenantId derived from context
            terminalId,
            drawId,
            ticketStatus,
            from,
            to
        );

        return new ListTicketsQuery(filter, PageRequest.of(page, size));
    }

    /**
     * Map TchPage<TicketSummaryDto> to TchPage<TicketSummaryResponse>.
     */
    public TchPage<TicketSummaryResponse> toPagedSummaryResponse(
        TchPage<ListTicketsQuery.TicketSummaryDto> page) {

        var items = page.items().stream()
            .map(dto -> new TicketSummaryResponse(
                dto.id().uuid(),
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
        return new TicketResponse.LineResponse(
            line.gameCode(),
            line.betType(),
            line.betOption(),
            line.selection(),
            line.stake(),
            line.oddsSnapshot(),
            line.potentialPayout()
        );
    }
}
