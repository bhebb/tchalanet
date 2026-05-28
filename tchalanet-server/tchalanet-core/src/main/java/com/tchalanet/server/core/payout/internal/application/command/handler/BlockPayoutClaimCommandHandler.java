package com.tchalanet.server.core.payout.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.payout.api.command.BlockPayoutClaimCommand;
import com.tchalanet.server.core.payout.api.command.PayoutWorkflowResult;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
public class BlockPayoutClaimCommandHandler
    implements CommandHandler<BlockPayoutClaimCommand, PayoutWorkflowResult> {

    private final PayoutReaderPort reader;
    private final PayoutWriterPort writer;
    private final Clock clock;

    @Override
    @TchTx
    public PayoutWorkflowResult handle(BlockPayoutClaimCommand command) {
        var claim = reader.getById(command.payoutId());
        var blocked = claim.block(command.blockedBy(), Instant.now(clock), command.blockReason());
        var saved = writer.save(blocked);
        return new PayoutWorkflowResult(saved.id(), saved.status(), saved.blockedAt());
    }
}
