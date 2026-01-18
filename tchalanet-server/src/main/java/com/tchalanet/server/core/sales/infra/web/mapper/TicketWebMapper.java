package com.tchalanet.server.core.sales.infra.web.mapper;

import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.sales.application.command.model.SellTicketCommand;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.domain.model.TicketLine;
import com.tchalanet.server.core.sales.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.infra.web.model.TicketResponse;
import org.springframework.stereotype.Component;

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
            ticket.getId().uuid(),
            ticket.getTenantId().uuid(),
            ticket.getTerminalId().uuid(),
            ticket.getDrawId().uuid(),
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

    private TicketResponse.LineResponse toLineResponse(TicketLine line) {
        return new TicketResponse.LineResponse(
            line.gameCode(),
            line.selection(),
            line.stake(),
            line.oddsSnapshot(),
            line.potentialPayout(),
            line.betType(),
            line.betOption() // ✅ expose it (useful for receipts + audits)
        );
    }
}
