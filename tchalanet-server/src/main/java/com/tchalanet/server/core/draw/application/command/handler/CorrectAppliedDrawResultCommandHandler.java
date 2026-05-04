package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.idempotency.event.ProcessedEventPort;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.application.command.model.CorrectAppliedDrawResultCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.application.port.out.DrawSalesGuardPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultCorrectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Handler pour corriger un résultat déjà appliqué à un draw.
 *
 * Flow :
 * 1. Require drawId, correctedDrawResultId, reason, idempotencyKey
 * 2. Lock/idempotency check
 * 3. Load Draw
 * 4. salesGuard.assertCanCorrectAppliedResult(...)
 * 5. Vérifier draw a déjà drawResultId
 * 6. Vérifier correctedDrawResultId différent de previous
 * 7. draw.overrideResult(correctedDrawResultId, now, reason)
 * 8. Save draw
 * 9. Marquer previous DrawResult comme OVERRIDDEN (TODO: implémenter)
 * 10. Publish DrawResultCorrectedEvent AFTER_COMMIT
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class CorrectAppliedDrawResultCommandHandler
    implements VoidCommandHandler<CorrectAppliedDrawResultCommand> {

    private final DrawLookupPort drawLookupPort;
    private final DrawLifecyclePort drawLifecyclePort;
    private final DrawSalesGuardPort salesGuard;
    private final DrawChannelCatalog drawChannelCatalog;
    private final ProcessedEventPort processedEventPort;
    private final DomainEventPublisher eventPublisher;
    private final IdGenerator idGenerator;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(CorrectAppliedDrawResultCommand command) {
        // 1. Require drawId, correctedDrawResultId, reason, idempotencyKey
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.correctedDrawResultId(), "correctedDrawResultId is required");

        if (command.reason() == null || command.reason().isBlank()) {
            throw ProblemRest.badRequest("draw.correct_result.reason_required");
        }

        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw ProblemRest.badRequest("draw.correct_result.idempotency_key_required");
        }

        // 2. Lock/idempotency check using ProcessedEventPort
        var idempotentEventId = UUID.nameUUIDFromBytes(
            ("correct-draw-result:" + command.drawId().value() + ":" + command.idempotencyKey())
                .getBytes()
        );

        if (processedEventPort.alreadyProcessed("CorrectAppliedDrawResult", idempotentEventId)) {
            log.warn("Idempotent duplicate request ignored: drawId={}, key={}",
                command.drawId(), command.idempotencyKey());
            return;
        }

        // 3. Load Draw
        var draw = drawLookupPort.getById(command.drawId());

        // 4. Sales guard validation
        salesGuard.assertCanCorrectAppliedResult(
            draw.id(),
            command.correctedDrawResultId(),
            command.force()
        );

        // 5. Vérifier draw a déjà drawResultId
        if (draw.drawResultId() == null) {
            throw ProblemRest.conflict("Cannot correct result: draw has no result applied yet");
        }

        var previousDrawResultId = draw.drawResultId();

        // 6. Vérifier correctedDrawResultId différent de previous
        if (previousDrawResultId.equals(command.correctedDrawResultId())) {
            throw ProblemRest.conflict("Corrected result is the same as current result");
        }

        // 7. draw.overrideResult(correctedDrawResultId, now, reason)
        var now = clock.instant();
        draw.overrideResult(
            command.correctedDrawResultId(),
            now,
            command.reason()
        );

        // 8. Save draw
        drawLifecyclePort.save(draw);

        // Mark idempotency as processed
        processedEventPort.markProcessed("CorrectAppliedDrawResult", idempotentEventId);

        log.info(
            "draw.result.corrected drawId={} previousResultId={} correctedResultId={} reason={}",
            draw.id(),
            previousDrawResultId,
            command.correctedDrawResultId(),
            command.reason()
        );

        // 10. Publish DrawResultCorrectedEvent AFTER_COMMIT
        // Note: DrawResult module listens to this event and marks previous result as OVERRIDDEN
        var eventTime = clock.instant();
        AfterCommit.run(() -> {
            // Fetch draw channel to get resultSlotId
            var channel = drawChannelCatalog
                .findById(draw.tenantId(), draw.drawChannelId())
                .orElseThrow(() -> new IllegalStateException(
                    "DrawChannel not found: " + draw.drawChannelId()));

            var event = new DrawResultCorrectedEvent(
                EventId.of(idGenerator.newUuid()),
                eventTime,
                draw.tenantId(),
                draw.id(),
                draw.drawDate(),
                channel.resultSlotId(),
                previousDrawResultId,
                command.correctedDrawResultId(),
                draw.drawChannelId(),
                command.reason()
            );
            eventPublisher.publish(event);
        });
    }
}



