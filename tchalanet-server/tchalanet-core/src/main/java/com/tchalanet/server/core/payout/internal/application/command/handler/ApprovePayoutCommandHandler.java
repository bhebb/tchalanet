package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.application.command.model.ApprovePayoutCommand;
import com.tchalanet.server.core.payout.application.command.model.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.event.PayoutApprovedEvent;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApprovePayoutCommandHandler implements CommandHandler<ApprovePayoutCommand, PayoutWorkflowResult> {
  private final PayoutReaderPort reader;
  private final PayoutWriterPort writer;
  private final DomainEventPublisher events;
  private final IdGenerator idGenerator;
  private final Clock clock;

  @Override
  @TchTx
  public PayoutWorkflowResult handle(ApprovePayoutCommand command) {
    var payout = reader.getById(command.payoutId());

    var now = Instant.now(clock);
    payout.approve(command.approvedBy(), now);
    var saved = writer.save(payout);

    var event = new PayoutApprovedEvent(
        EventId.of(idGenerator.newUuid()),
        now,
        command.tenantId(),
        saved.getId(),
        saved.getTicketId(),
        saved.getAmountCents(),
        saved.getCurrency(),
        command.approvedBy());
    AfterCommit.run(() -> events.publish(event));

    return new PayoutWorkflowResult(saved.getId(), saved.getStatus(), now);
  }
}
