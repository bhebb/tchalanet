package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.drawresult.api.command.ConfirmDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.ConfirmDrawResultResult;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ConfirmDrawResultCommandHandler
    implements CommandHandler<ConfirmDrawResultCommand, ConfirmDrawResultResult> {

    private final DrawResultWriterPort writer;
    private final Clock clock;

    @Override
    public ConfirmDrawResultResult handle(ConfirmDrawResultCommand command) {
        var confirmedAt = Instant.now(clock);
        writer.confirmProvisional(command.drawResultId(), confirmedAt);

        log.info("draw_result.confirmed drawResultId={} confirmedBy={}",
            command.drawResultId(), command.confirmedBy());

        return new ConfirmDrawResultResult(command.drawResultId(), true);
    }
}
