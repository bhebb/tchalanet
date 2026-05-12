package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.UnlockDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UnlockDrawCommandHandler implements VoidCommandHandler<UnlockDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(UnlockDrawCommand command) {
        log.info("Unlocking draw {}", command.drawId());
        Objects.requireNonNull(command.drawId(), "drawId is required");

        Draw draw = drawLookupPort.getById(command.drawId());

        draw.unlock();

        drawLifecyclePort.save(draw);
    }
}
