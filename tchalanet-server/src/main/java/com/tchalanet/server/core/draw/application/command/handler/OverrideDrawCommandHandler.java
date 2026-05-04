package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.OverrideDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OverrideDrawCommandHandler implements VoidCommandHandler<OverrideDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(OverrideDrawCommand command) {
        log.info("Overriding draw {} for reason: {}", command.drawId(), command.reason());
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.reason(), "reason is required");

        Draw draw = drawLookupPort.getById(command.drawId());

        // Logique d'override si nécessaire dans le domaine

        drawLifecyclePort.save(draw);
    }
}
