package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.api.command.ExecutePayoutCommand;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.internal.domain.event.PayoutPaidEvent;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ExecutePayoutCommandHandler
    implements CommandHandler<ExecutePayoutCommand, PayoutWorkflowResult> {

    private final PayoutReaderPort reader;
    private final PayoutWriterPort writer;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public PayoutWorkflowResult handle(ExecutePayoutCommand command) {
        var payout =
            reader
                .getById(command.payoutId());

        if (payout.isPaid()) {
            return new PayoutWorkflowResult(payout.getId(), payout.getStatus(), payout.getPaidAt());
        }

        var now = Instant.now(clock);

        payout.markPaid(
            command.paidBy(),
            command.payingOutletId(),
            command.payingSessionId(),
            command.terminalId(),
            now,
            false);

        var saved = writer.save(payout);

        var event =
            new PayoutPaidEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                command.tenantId(),
                saved.getId(),
                saved.getTicketId(),
                saved.getAmountCents(),
                saved.getCurrency(),
                command.paidBy(),
                command.payingSessionId(),
                command.payingOutletId(),
                command.terminalId());

        AfterCommit.run(() -> events.publish(event));

        return new PayoutWorkflowResult(saved.getId(), saved.getStatus(), saved.getPaidAt());
    }
}
