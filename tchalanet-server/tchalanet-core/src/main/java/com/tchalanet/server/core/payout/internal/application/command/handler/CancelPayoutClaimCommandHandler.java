package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.api.command.CancelPayoutClaimCommand;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class CancelPayoutClaimCommandHandler
    implements CommandHandler<CancelPayoutClaimCommand, PayoutWorkflowResult> {

    private final PayoutReaderPort reader;
    private final PayoutWriterPort writer;
    private final Clock clock;

    @Override
    @TchTx
    public PayoutWorkflowResult handle(CancelPayoutClaimCommand command) {
        var claim = reader.getById(command.payoutId());
        var cancelled = claim.cancel(command.cancelledBy(), Instant.now(clock), command.cancelReason());
        var saved = writer.save(cancelled);
        return new PayoutWorkflowResult(saved.id(), saved.status(), saved.cancelledAt());
    }
}
