package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.OpenDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDrawCommandHandler implements VoidCommandHandler<OpenDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(OpenDrawCommand command) {
        var drawIds = DrawLifecycleCommandGuard.requireDrawIds(command.drawIds());
        log.info("Opening {} draw(s)", drawIds.size());

        var now = clock.instant();
        for (var drawId : drawIds) {
            var draw = drawLookupPort.getById(drawId);
            draw.open(now);
            drawLifecyclePort.save(draw);
        }
    }
}
