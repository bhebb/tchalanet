package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RescheduleDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.model.Draw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RescheduleDrawCommandHandler implements VoidCommandHandler<RescheduleDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;

    @Override
    @TchTx
    public void handle(RescheduleDrawCommand command) {
        log.info("Rescheduling draw {} to {}", command.drawId(), command.scheduledAt());
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.scheduledAt(), "scheduledAt is required");
        Objects.requireNonNull(command.cutoffAt(), "cutoffAt is required");

        Draw draw = drawLookupPort.getById(command.drawId());

        draw.reschedule(draw.drawDate(), command.scheduledAt(), command.cutoffAt());

        drawLifecyclePort.save(draw);
    }
}
