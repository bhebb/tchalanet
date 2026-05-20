package com.tchalanet.server.core.sales.internal.application.command.handler.print;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.status.TicketSaleStatus;
import com.tchalanet.server.core.sales.internal.application.command.model.ReprintTicketCommand;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ReprintTicketCommandHandler implements CommandHandler<ReprintTicketCommand, Void> {

    private final TicketReaderPort reader;

    @Override
    @TchTx
    public Void handle(ReprintTicketCommand command) {
        var ticket = reader.getRequired(command.ticketId());
        if (!ticket.print().printed()) {
            throw new IllegalStateException("Ticket has not been printed yet — cannot reprint");
        }
        if (ticket.lifecycle().sale().status() == TicketSaleStatus.CANCELLED || ticket.lifecycle().sale().status() == TicketSaleStatus.VOIDED) {
            throw new IllegalStateException("Cannot reprint a " + ticket.lifecycle().sale().status() + " ticket");
        }
        return null;
    }
}
