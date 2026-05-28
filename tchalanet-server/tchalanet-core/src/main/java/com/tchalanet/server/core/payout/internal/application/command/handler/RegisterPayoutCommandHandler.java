package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutResult;
import lombok.RequiredArgsConstructor;

/**
 * Retained as stub. RegisterPayoutCommand is no longer used in V1.
 * Payout claims are opened automatically from TicketWinningSettlementCreatedEvent.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterPayoutCommandHandler
    implements CommandHandler<RegisterPayoutCommand, RegisterPayoutResult> {

    @Override
    @TchTx
    public RegisterPayoutResult handle(RegisterPayoutCommand command) {
        throw new UnsupportedOperationException(
            "RegisterPayoutCommand is removed in V1. Payout claims are created from settlement events.");
    }
}
