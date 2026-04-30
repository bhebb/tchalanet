package com.tchalanet.server.core.sales.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.error.ProblemRestException;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.sales.application.command.model.OverrideTicketResultCommand;
import com.tchalanet.server.core.sales.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.domain.event.TicketResultOverriddenEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@UseCase
@RequiredArgsConstructor
public class OverrideTicketResultCommandHandler implements CommandHandler<OverrideTicketResultCommand, Void> {

  private final TicketReaderPort ticketReader;
  private final TicketWriterPort ticketWriter;
  private final DomainEventPublisher publisher;
  private final Clock clock;

  @Override
  @TchTx
  @PreAuthorize("hasPermission('ticket.result.override')")
  public Void handle(OverrideTicketResultCommand cmd) {
    var ticket =
        ticketReader.findWithLinesById(cmd.ticketId())
            .orElseThrow(() -> ProblemRestException.notFound("Ticket not found"));

    Instant when = cmd.performedAt() != null ? cmd.performedAt() : Instant.now(clock);
    if (cmd.totalPayout() == null || cmd.totalPayout().signum() < 0) {
      throw new IllegalArgumentException("totalPayout must be >= 0");
    }
    var providedResultStatus = cmd.status() != null ? cmd.status().resultStatus() : null;
    if ((providedResultStatus != com.tchalanet.server.common.types.enums.TicketResultStatus.WON && providedResultStatus != com.tchalanet.server.common.types.enums.TicketResultStatus.LOST)) {
      throw new IllegalArgumentException("status must be WON or LOST");
    }

    // Domain method: forceResult with explicit result status
    ticket.forceResult(cmd.totalPayout(), providedResultStatus, when);

    var saved = ticketWriter.save(ticket);

    AfterCommit.run(
        () ->
            publisher.publish(
                new TicketResultOverriddenEvent(
                    com.tchalanet.server.common.types.id.EventId.of(UUID.randomUUID()),
                    when,
                    saved.getTenantId(),
                    saved.getId(),
                    saved.getDrawId(),
                    saved.getWinningAmount(),
                    providedResultStatus,
                    cmd.reason(),
                    cmd.performedBy())));

    return null;
  }
}
