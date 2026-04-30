package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.audit.infra.web.AuditLog;
import com.tchalanet.server.core.draw.application.command.model.SettleDrawCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.event.DrawSettledEvent;
import com.tchalanet.server.core.draw.domain.exception.DrawResultNotFinalException;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Use case pour régler un tirage : - charger les tickets, - calculer gains/pertes/commissions, -
 * persister les mouvements et soldes, - marquer le tirage comme SETTLED, - gérer les invalidations
 * / refresh caches.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class SettleDrawsCommandHandler implements VoidCommandHandler<SettleDrawCommand> {

    private final DrawLookupPort drawReaderPort;
    private final DrawLifecyclePort drawWriterPort;
    private final DrawResultReaderPort drawResultReaderPort;
    private final DomainEventPublisher publisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    @Override
    @TchTx
    @PreAuthorize("hasPermission('draw.settle')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.SETTLE,
        idExpression = "#command.drawId.toString()")
    public void handle(SettleDrawCommand command) {
        var draw =
            drawReaderPort
                .findById(command.drawId())
                .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + command.drawId()));

        if (draw.drawResultId() == null) {
            throw new IllegalStateException("Draw " + draw.id() + " has no result attached");
        }

        // [Règle Settle FINAL only]
        var drawResult = drawResultReaderPort.getById(draw.drawResultId());
        if (drawResult.status() != DrawResultStatus.FINAL) {
            throw new DrawResultNotFinalException(draw.id(), draw.drawResultId());
        }

        var wasResulted =
            draw.status() == com.tchalanet.server.core.draw.domain.model.DrawStatus.RESULTED;
        draw.settle(ZonedDateTime.now(clock));
        drawWriterPort.save(draw);

        if (wasResulted) {
            var event =
                new DrawSettledEvent(
                    EventId.of(idGenerator.newUuid()),
                    Instant.now(clock),
                    draw.tenantId(),
                    draw.id(),
                    draw.drawChannel().code(),
                    draw.scheduledAt().toInstant(),
                    draw.drawChannel().code());
            AfterCommit.run(() -> publisher.publish(event));
        }
    }
}
