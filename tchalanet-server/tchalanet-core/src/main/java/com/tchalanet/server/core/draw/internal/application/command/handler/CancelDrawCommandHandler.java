package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.command.CancelDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSalesGuardPort;
import com.tchalanet.server.core.draw.internal.domain.event.DrawCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CancelDrawCommandHandler implements VoidCommandHandler<CancelDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;
    private final DrawSalesGuardPort salesGuard;
    private final Clock clock;
    private final DomainEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public void handle(CancelDrawCommand command) {
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.reason(), "reason is required");

        var draw = drawLookupPort.getById(command.drawId());

        // Validate that draw can be cancelled (checks sales, payouts, etc.)
        salesGuard.assertCanCancel(draw.id(), command.force());

        draw.cancel(command.reason(), clock.instant());

        drawLifecyclePort.save(draw);

        var eventTime = clock.instant();
        AfterCommit.run(() -> eventPublisher.publish(new DrawCancelledEvent(
            EventId.of(idGenerator.newUuid()),
            eventTime,
            draw.tenantId(),
            draw.id(),
            draw.drawChannelId(),
            draw.drawDate(),
            command.reason()
        )));
    }
}
