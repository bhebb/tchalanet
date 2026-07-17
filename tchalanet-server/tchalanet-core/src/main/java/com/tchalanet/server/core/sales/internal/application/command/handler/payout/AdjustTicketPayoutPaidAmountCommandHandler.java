package com.tchalanet.server.core.sales.internal.application.command.handler.payout;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.sales.api.command.payout.AdjustTicketPayoutPaidAmountCommand;
import com.tchalanet.server.core.sales.api.command.payout.AdjustTicketPayoutPaidAmountResult;
import com.tchalanet.server.core.sales.api.event.TicketPayoutPaidAmountAdjustedEvent;
import com.tchalanet.server.core.sales.internal.application.port.out.TicketReaderPort;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class AdjustTicketPayoutPaidAmountCommandHandler
    implements CommandHandler<
        AdjustTicketPayoutPaidAmountCommand, AdjustTicketPayoutPaidAmountResult> {

  private final TicketReaderPort ticketReader;
  private final DomainEventPublisher eventPublisher;
  private final IdGenerator idGenerator;

  @Override
  @TchTx
  public AdjustTicketPayoutPaidAmountResult handle(AdjustTicketPayoutPaidAmountCommand command) {
    Objects.requireNonNull(command, "command is required");
    if (command.previousPaidAmount().signum() < 0 || command.adjustedPaidAmount().signum() < 0) {
      throw new IllegalArgumentException("payout.adjustment.amount_must_be_non_negative");
    }

    var ticket = ticketReader.getRequired(command.ticketId());
    if (!ticket.identity().tenantId().equals(command.tenantId())) {
      throw new IllegalArgumentException("Ticket does not belong to tenant " + command.tenantId());
    }
    if (command.previousPaidAmount().signum() <= 0) {
      throw new IllegalArgumentException("payout.adjustment.previous_amount_must_be_positive");
    }

    long previousPaidAmountCents = toCents(command.previousPaidAmount());
    long adjustedPaidAmountCents = toCents(command.adjustedPaidAmount());
    long deltaAmountCents = adjustedPaidAmountCents - previousPaidAmountCents;
    var calculatedAmount = ticket.winningAmount().amount();
    long calculatedAmountCents =
        calculatedAmount != null && calculatedAmount.signum() > 0
            ? toCents(calculatedAmount)
            : previousPaidAmountCents;

    var event =
        new TicketPayoutPaidAmountAdjustedEvent(
            EventId.of(idGenerator.newUuid()),
            command.adjustedAt(),
            ticket.identity().tenantId(),
            ticket.identity().id(),
            ticket.context().drawId(),
            calculatedAmountCents,
            previousPaidAmountCents,
            adjustedPaidAmountCents,
            deltaAmountCents,
            ticket.money().currency().code(),
            ticket.context().sellerTerminalId(),
            command.reason(),
            command.adjustedBy());

    AfterCommit.run(() -> eventPublisher.publish(event));

    return new AdjustTicketPayoutPaidAmountResult(
        ticket.identity().id(),
        calculatedAmountCents,
        previousPaidAmountCents,
        adjustedPaidAmountCents,
        deltaAmountCents);
  }

  private static long toCents(BigDecimal amount) {
    return amount == null ? 0L : amount.movePointRight(2).longValueExact();
  }
}
