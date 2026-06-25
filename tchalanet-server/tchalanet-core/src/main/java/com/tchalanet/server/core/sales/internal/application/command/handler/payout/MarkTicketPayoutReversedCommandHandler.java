package com.tchalanet.server.core.sales.internal.application.command.handler.payout;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutReversedCommand;
import com.tchalanet.server.core.sales.api.command.payout.MarkTicketPayoutReversedResult;
import com.tchalanet.server.core.sales.api.event.TicketPayoutReversedEvent;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketWriterPort;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkTicketPayoutReversedCommandHandler
    implements CommandHandler<MarkTicketPayoutReversedCommand, MarkTicketPayoutReversedResult> {

  private final TicketReaderPort ticketReader;
  private final TicketWriterPort ticketWriter;
  private final DomainEventPublisher eventPublisher;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public MarkTicketPayoutReversedResult handle(MarkTicketPayoutReversedCommand command) {
    var ticket = ticketReader.getRequired(command.ticketId());
    if (!ticket.identity().tenantId().equals(command.tenantId())) {
      throw new IllegalArgumentException("Ticket does not belong to tenant " + command.tenantId());
    }

    var reversed = ticket.markPayoutReversed(command.reversedBy(), command.reversedAt());
    var saved = ticketWriter.save(reversed);
    var amount = saved.winningAmount().amount();

    var event = new TicketPayoutReversedEvent(
        EventId.of(idGenerator.newUuid()),
        command.reversedAt(),
        saved.identity().tenantId(),
        saved.identity().id(),
        saved.context().drawId(),
        toCents(amount),
        saved.money().currency().code(),
        saved.context().sellerTerminalId(),
        command.reversedBy());

    AfterCommit.run(() -> eventPublisher.publish(event));

    return new MarkTicketPayoutReversedResult(
        saved.identity().id(),
        saved.lifecycle().settlement().status());
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.movePointRight(2).longValueExact();
  }
}
