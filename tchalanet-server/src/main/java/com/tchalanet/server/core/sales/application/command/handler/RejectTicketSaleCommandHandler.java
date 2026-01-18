package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.sales.application.command.model.RejectTicketSaleCommand;
import com.tchalanet.server.core.sales.application.command.model.TicketRejectedResult;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RejectTicketSaleCommandHandler
    implements CommandHandler<RejectTicketSaleCommand, TicketRejectedResult> {

  private final TicketReaderPort ticketReader;
  private final TicketWritterPort ticketWriter;
  private final Clock clock;

  @Override
  @TchTx
  public TicketRejectedResult handle(RejectTicketSaleCommand cmd) {
    var ticket =
        ticketReader
            .findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

    if (ticket.getStatus() != TicketStatus.PENDING_APPROVAL) {
      throw new IllegalStateException("Ticket is not pending approval. status=" + ticket.getStatus());
    }

    Instant now = Instant.now(clock);
    ticket.reject(now);

    var saved = ticketWriter.save(ticket);
    log.info("Ticket rejected ticketId={} status={}", saved.getId(), saved.getStatus());
    return new TicketRejectedResult(saved);
  }
}
