package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.internal.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.internal.domain.exception.DrawResultNotFinalException;
import com.tchalanet.server.core.draw.internal.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class SettleDrawCommandHandler implements VoidCommandHandler<SettleDrawCommand> {

    private final DrawSummaryReaderPort drawSummaryReaderPort;
    private final DrawLifecyclePort drawLifecyclePort;
    private final DrawLookupPort drawLookupPort;
    private final Clock clock;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    public void handle(SettleDrawCommand command) {
        var drawSummary = drawSummaryReaderPort.getById(command.drawId());

        var result = drawSummary.result();

        if (result == null || result.id() == null) {
            throw new DrawResultNotFinalException(drawSummary.drawId(), null);
        }


        if (DrawResultStatus.CONFIRMED != result.status()) {
            throw new DrawResultNotFinalException(drawSummary.drawId(), result.id());
        }

        boolean wasResulted = drawSummary.status() == DrawStatus.RESULTED;
        Instant now = clock.instant();

        var draw = drawLookupPort.getByIdForUpdate(drawSummary.drawId());

        draw.settle(now);

        drawLifecyclePort.save(draw);

        if (wasResulted) {
            var event = new DrawSettledEvent(
                EventId.of(idGenerator.newUuid()),
                now,
                draw.tenantId(),
                draw.id(),
                draw.drawChannelId(),
                drawSummary.resultSlotId(),
                draw.drawResultId(),
                draw.drawDate(),
                draw.scheduledAt()
            );

            AfterCommit.run(() -> publisher.publish(event));
        }
    }
}
