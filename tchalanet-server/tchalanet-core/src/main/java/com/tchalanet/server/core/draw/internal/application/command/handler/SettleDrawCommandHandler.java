package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.core.draw.api.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.api.model.DrawStatus;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSummaryReaderPort;
import com.tchalanet.server.core.draw.internal.domain.exception.DrawResultNotFinalException;
import com.tchalanet.server.core.drawresult.api.model.DrawResultStatus;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    var drawIds = DrawLifecycleCommandGuard.requireDrawIds(command.drawIds());

    for (var drawId : drawIds) {
      settle(drawId);
    }
  }

  private void settle(DrawId drawId) {
    var drawSummary = drawSummaryReaderPort.getById(drawId);

    // Idempotent: an already-settled draw is a graceful no-op — no re-settle, no
    // duplicate money effect, no duplicate event. Makes SettleDrawCommand safe to
    // retry (a batch job re-running the same draw does nothing the second time).
    if (drawSummary.status() == DrawStatus.SETTLED) {
      log.debug("draw.settle skip already-settled drawId={}", drawSummary.drawId());
      return;
    }

    var result = drawSummary.result();

    if (result == null || result.id() == null) {
      throw new DrawResultNotFinalException(drawSummary.drawId(), null);
    }

    if (DrawResultStatus.CONFIRMED != result.status()
        && DrawResultStatus.OVERRIDDEN != result.status()) {
      throw new DrawResultNotFinalException(drawSummary.drawId(), result.id());
    }

    Instant now = clock.instant();

    var draw = drawLookupPort.getByIdForUpdate(drawSummary.drawId());

    // Only a RESULTED draw reaches here: SETTLED is handled above, and any other
    // state is rejected by the result checks or by the transition guard in settle().
    draw.settle(now);

    drawLifecyclePort.save(draw);

    var event =
        new DrawSettledEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            draw.tenantId(),
            draw.id(),
            draw.drawChannelId(),
            drawSummary.resultSlotId(),
            draw.drawResultId(),
            draw.drawDate(),
            draw.scheduledAt());

    AfterCommit.run(() -> publisher.publish(event));
  }
}
