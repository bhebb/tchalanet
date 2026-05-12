package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.ArchiveDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class ArchiveDrawCommandHandler implements VoidCommandHandler<ArchiveDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(ArchiveDrawCommand command) {
        log.info("Archiving draw {}", command.drawId());
        Objects.requireNonNull(command.drawId(), "drawId is required");

        var draw = drawLookupPort.getById(command.drawId());

        draw.archive();

        drawLifecyclePort.save(draw);
    }
}
