package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.api.command.RejectPayoutCommand;
import lombok.RequiredArgsConstructor;

/**
 * Retained as stub. Rejection workflow is removed in V1.
 * Use CancelPayoutClaimCommand for admin cancellations.
 */
@UseCase
@RequiredArgsConstructor
public class RejectPayoutCommandHandler
    implements CommandHandler<RejectPayoutCommand, PayoutWorkflowResult> {

    @Override
    @TchTx
    public PayoutWorkflowResult handle(RejectPayoutCommand command) {
        throw new UnsupportedOperationException(
            "RejectPayoutCommand is removed in V1. Use CancelPayoutClaimCommand instead.");
    }
}
