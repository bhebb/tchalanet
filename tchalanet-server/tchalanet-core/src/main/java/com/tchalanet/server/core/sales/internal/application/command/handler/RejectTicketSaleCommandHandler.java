package com.tchalanet.server.core.sales.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.web.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.api.model.TicketSaleStatus;
import com.tchalanet.server.core.sales.api.command.RejectTicketSaleCommand;
import com.tchalanet.server.core.sales.api.command.TicketRejectedResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RejectTicketSaleCommandHandler
    implements CommandHandler<RejectTicketSaleCommand, TicketRejectedResult> {

    private final TicketReaderPort ticketReader;
    private final TicketWriterPort ticketWriter;
    private final Clock clock;

    @Override
    @TchTx
    public TicketRejectedResult handle(RejectTicketSaleCommand cmd) {
        var ticket =
            ticketReader
                .findWithLinesById(cmd.ticketId())
                .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

        if (ticket.getSaleStatus() != TicketSaleStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Ticket is not pending approval. status=" + ticket.getSaleStatus());
        }

        Instant now = Instant.now(clock);
        ticket.reject(now);

        var saved = ticketWriter.save(ticket);
        log.info("Ticket rejected ticketId={} status={}", saved.getId(), saved.getSaleStatus());
        return new TicketRejectedResult(saved);
    }
}
