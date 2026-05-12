package com.tchalanet.server.core.sales.internal.application.sell;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.MarkTicketPayoutPaidCommand;
import com.tchalanet.server.core.sales.api.command.MarkTicketPayoutPaidResult;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import com.tchalanet.server.core.sales.internal.domain.event.TicketPayoutMarkedPaidEvent;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkTicketPayoutPaidCommandHandler
    implements CommandHandler<MarkTicketPayoutPaidCommand, MarkTicketPayoutPaidResult> {

  private final TicketReaderPort reader;
  private final TicketWriterPort writer;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public MarkTicketPayoutPaidResult handle(MarkTicketPayoutPaidCommand cmd) {
    var ticket = reader.findById(cmd.ticketId())
        .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + cmd.ticketId()));

    if (ticket.paidOut()) {
      return new MarkTicketPayoutPaidResult(ticket, false);
    }

    var marked = ticket.markPaid(cmd.paidBy(), clock.instant());
    var saved = writer.save(marked);

    AfterCommit.run(() -> events.publish(new TicketPayoutMarkedPaidEvent(
        EventId.of(idGenerator.newUuid()),
        clock.instant(),
        cmd.tenantId(),
        saved.id(),
        cmd.paidBy(),
        cmd.currency(),
        cmd.source())));

    return new MarkTicketPayoutPaidResult(saved, true);
  }
}
