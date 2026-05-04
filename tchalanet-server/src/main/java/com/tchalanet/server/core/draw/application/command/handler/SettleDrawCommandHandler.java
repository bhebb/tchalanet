package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.domain.exception.DrawResultNotFinalException;
import com.tchalanet.server.core.draw.domain.model.Draw;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SettleDrawCommandHandler implements VoidCommandHandler<SettleDrawCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;
    private final Clock clock;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final DrawResultReaderPort drawResultReaderPort;

    @Override
    @TchTx
    public void handle(SettleDrawCommand command) {
        Objects.requireNonNull(command.drawId(), "drawId is required");

        Draw draw = drawLookupPort.getById(command.drawId());

        if (draw.drawResultId() == null) {
            throw new DrawResultNotFinalException(draw.id(), null);
        }

        var drawResult = drawResultReaderPort.getById(draw.drawResultId());
        if (drawResult.status() != DrawResultStatus.CONFIRMED) {
            throw new DrawResultNotFinalException(draw.id(), draw.drawResultId());
        }

        boolean wasResulted = draw.status() == DrawStatus.RESULTED;
        Instant now = clock.instant();

        draw.settle(now);
        drawLifecyclePort.save(draw);

        if (wasResulted) {
            var event = new DrawSettledEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                draw.tenantId(),
                draw.id(),
                draw.drawChannelId(),
                null, // TODO resultSlotId à ajouter via draw snapshot/lookup
                draw.drawResultId(),
                draw.drawDate(),
                draw.scheduledAt()
            );

            AfterCommit.run(() -> publisher.publish(event));
        }
    }
}
