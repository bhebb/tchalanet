package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.LockDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class LockDrawCommandHandler implements VoidCommandHandler<LockDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(LockDrawCommand command) {
        var drawIds = DrawLifecycleCommandGuard.requireDrawIds(command.drawIds());
        log.info("Locking {} draw(s)", drawIds.size());

        for (var drawId : drawIds) {
            Draw draw = drawLookupPort.getById(drawId);

            draw.lock();

            drawLifecyclePort.save(draw);
        }
    }

}
