package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.api.command.ApprovePayoutCommand;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import lombok.RequiredArgsConstructor;

/**
 * Retained as stub. Approval workflow is removed in V1.
 * Use BlockPayoutClaimCommand / UnblockPayoutClaimCommand for manual controls.
 */
@UseCase
@RequiredArgsConstructor
public class ApprovePayoutCommandHandler
    implements CommandHandler<ApprovePayoutCommand, PayoutWorkflowResult> {

    @Override
    @TchTx
    public PayoutWorkflowResult handle(ApprovePayoutCommand command) {
        throw new UnsupportedOperationException(
            "ApprovePayoutCommand is removed in V1. Use block/unblock for claim controls.");
    }
}
