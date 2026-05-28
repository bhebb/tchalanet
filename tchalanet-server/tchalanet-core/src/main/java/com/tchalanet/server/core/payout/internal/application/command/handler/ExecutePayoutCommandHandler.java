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
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaimStatus;
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
        // Acquire pessimistic lock to prevent concurrent double-pay
        var payout = writer.lockByIdForPayment(command.payoutId());

        if (payout.status() == PayoutClaimStatus.PAID) {
            return new PayoutWorkflowResult(payout.id(), payout.status(), payout.paidAt());
        }

        var now = Instant.now(clock);

        var paid = payout.pay(
            command.payingOutletId(),
            command.payingSessionId(),
            command.terminalId(),
            command.paidBy(),
            now);

        var saved = writer.save(paid);

        var event =
            new PayoutPaidEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                command.tenantId(),
                saved.id(),
                saved.ticketId(),
                saved.amountCents(),
                saved.currency(),
                command.paidBy(),
                command.payingSessionId(),
                command.payingOutletId(),
                command.terminalId());

        AfterCommit.run(() -> events.publish(event));

        return new PayoutWorkflowResult(saved.id(), saved.status(), saved.paidAt());
    }
}
