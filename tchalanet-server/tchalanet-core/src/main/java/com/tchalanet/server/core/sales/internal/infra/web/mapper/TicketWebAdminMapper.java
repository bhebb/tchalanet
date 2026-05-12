package com.tchalanet.server.core.sales.internal.infra.web.mapper;

import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.core.sales.api.command.SellTicketCommand;
import com.tchalanet.server.core.sales.api.command.SellTicketResult;
import com.tchalanet.server.core.sales.internal.domain.model.Ticket;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketLineRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.SellTicketRequest;
import com.tchalanet.server.core.sales.internal.infra.web.model.TicketResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketWebAdminMapper {

    default SellTicketCommand toSellCommand(TchRequestContext ctx, SellTicketRequest request) {
        return new SellTicketCommand(
            ctx.effectiveTenantIdRequired(),
            ctx.terminalIdRequired(), // TODO adapt if id comes from request or current POS context.
            ctx.userId(),
            CurrencyCode.of("HTG"),
            request.feeAmount(),
            request.lines().stream()
                .map(l -> new SellTicketLineRequest(
                    l.gameCode(),
                    l.selection(),
                    l.betType(),
                    l.betOption(),
                    l.stakeAmount(),
                    l.oddsSnapshot()))
                .toList());
    }

    default TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
            ticket.id().value().toString(),
            ticket.publicCode(),
            ticket.saleStatus().name(),
            ticket.saleOrigin().name(),
            ticket.syncStatus().name(),
            ticket.money().stakeAmount(),
            ticket.money().feeAmount(),
            ticket.money().totalAmount());
    }

    default TicketResponse toTicketResponse(SellTicketResult result) {
        return toTicketResponse(result.ticket());
    }
}
