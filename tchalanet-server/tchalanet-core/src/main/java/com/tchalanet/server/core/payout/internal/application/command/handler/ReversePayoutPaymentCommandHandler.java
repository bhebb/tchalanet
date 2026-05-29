package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.api.command.ReversePayoutPaymentCommand;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.internal.domain.event.PayoutReversedEvent;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class ReversePayoutPaymentCommandHandler
    implements CommandHandler<ReversePayoutPaymentCommand, PayoutWorkflowResult> {

    private final PayoutReaderPort reader;
    private final PayoutWriterPort writer;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public PayoutWorkflowResult handle(ReversePayoutPaymentCommand command) {
        var claim = reader.getById(command.payoutId());
        var now = Instant.now(clock);
        var reversed = claim.reverse(command.reversedBy(), now, command.reverseReason());
        var saved = writer.save(reversed);

        var event = new PayoutReversedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            saved.tenantId(),
            saved.id(),
            saved.ticketId(),
            saved.amountCents(),
            saved.currency(),
            command.reversedBy(),
            command.reverseReason());

        AfterCommit.run(() -> events.publish(event));

        return new PayoutWorkflowResult(saved.id(), saved.status(), saved.reversedAt());
    }
}
