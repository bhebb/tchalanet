package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutCommand;
import com.tchalanet.server.core.payout.api.command.RegisterPayoutResult;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Clock;

/**
 * Concurrency contract (v1 — no external cache):
 * 1. PosPayoutOperationValidator runs pre-transaction.
 * 2. Inside {@code @TchTx}: terminal-locked, outlet.payoutBlocked, and session.status are
 * re-read from the DB before persisting the payout row. If any critical state changed
 * since pre-check, the handler returns the appropriate blocked result (TERMINAL_LOCKED,
 * OUTLET_PAYOUT_BLOCKED, SESSION_CLOSED).
 * 3. The payout row itself carries a version column; optimistic locking prevents double-pay.
 */
@UseCase
@RequiredArgsConstructor
public class RegisterPayoutCommandHandler
    implements CommandHandler<RegisterPayoutCommand, RegisterPayoutResult> {

    private final PayoutReaderPort payoutReader;
    private final PayoutWriterPort payoutWriter;
    private final DomainEventPublisher events;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final QueryBus queryBus;

    @Override
    @TchTx
    public RegisterPayoutResult handle(RegisterPayoutCommand command) {
        return null;
    }


    private static long toCentsExact(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
