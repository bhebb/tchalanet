package com.tchalanet.server.core.sales.internal.infra.web.mapper;

import com.tchalanet.server.common.context.TchRequestContext;
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
        return null;
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
