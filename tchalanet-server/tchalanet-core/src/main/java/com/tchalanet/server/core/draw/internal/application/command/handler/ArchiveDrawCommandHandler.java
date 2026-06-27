package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.ArchiveDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveDrawCommandHandler implements VoidCommandHandler<ArchiveDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(ArchiveDrawCommand command) {
        var drawIds = DrawLifecycleCommandGuard.requireDrawIds(command.drawIds());
        log.info("Archiving {} draw(s)", drawIds.size());

        for (var drawId : drawIds) {
            var draw = drawLookupPort.getById(drawId);

            draw.archive();

            drawLifecyclePort.save(draw);
        }
    }

}
