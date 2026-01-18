package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.accesscontrol.application.annotation.RequiresPermission;
import com.tchalanet.server.core.sales.application.command.model.OverrideTicketResultCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWritterPort;
import com.tchalanet.server.core.sales.domain.event.TicketResultOverriddenEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OverrideTicketResultCommandHandler implements CommandHandler<OverrideTicketResultCommand, Void> {

  private final TicketReaderPort ticketReader;
  private final TicketWritterPort ticketWriter;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  @RequiresPermission("ticket.result.override")
  public Void handle(OverrideTicketResultCommand cmd) {
    var ticket =
        ticketReader.findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

    Instant when = cmd.performedAt() != null ? cmd.performedAt() : Instant.now(clock);
    if (cmd.totalPayout() == null || cmd.totalPayout().signum() < 0) {
      throw new IllegalArgumentException("totalPayout must be >= 0");
    }
    if (cmd.getTicketResultStatus() != RESULTED_WON && cmd.status() != TicketStatus.RESULTED_LOST) {
      throw new IllegalArgumentException("status must be RESULTED_WON or RESULTED_LOST");
    }

    // Domain method: forceResult
    ticket.forceResult(cmd.totalPayout(), cmd.status(), when);

    var saved = ticketWriter.save(ticket);

    AfterCommit.run(
        () ->
            publisher.publish(
                new TicketResultOverriddenEvent(
                    UUID.randomUUID(),
                    when,
                    saved.getTenantId(),
                    saved.getId(),
                    saved.getDrawId(),
                    saved.getWinningAmount(),
                    cmd.status(),
                    cmd.reason(),
                    cmd.performedBy())));

    return null;
  }
}

