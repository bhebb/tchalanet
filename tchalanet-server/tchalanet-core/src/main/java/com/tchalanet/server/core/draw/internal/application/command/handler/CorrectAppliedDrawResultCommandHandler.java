package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.DrawChannelCatalog;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.platform.idempotence.api.ProcessedEventPort;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.core.draw.api.command.CorrectAppliedDrawResultCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawSalesGuardPort;
import com.tchalanet.server.core.draw.api.event.DrawResultCorrectedEvent;
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

    private static final String HANDLER_KEY = "draw.correct-applied-result";

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
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.drawId(), "drawId is required");
        Objects.requireNonNull(command.correctedDrawResultId(), "correctedDrawResultId is required");

        if (command.reason() == null || command.reason().isBlank()) {
            throw ProblemRest.badRequest("draw.correct_result.reason_required");
        }
        if (command.reason().trim().length() < 10) {
            throw ProblemRest.badRequest("draw.correct_result.reason_too_short");
        }

        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw ProblemRest.badRequest("draw.correct_result.idempotency_key_required");
        }

        var idempotentEventId = UUID.nameUUIDFromBytes(
            ("correct-draw-result:" + command.drawId().value() + ":" + command.idempotencyKey())
                .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        if (processedEventPort.alreadyProcessed(HANDLER_KEY, idempotentEventId)) {
            log.warn("draw.correct_result duplicate ignored drawId={} key={}",
                command.drawId(), command.idempotencyKey());
            return;
        }

        var draw = drawLookupPort.getById(command.drawId());

        if (draw.drawResultId() == null) {
            throw ProblemRest.conflict("draw.correct_result.no_result_applied");
        }

        var previousDrawResultId = draw.drawResultId();

        if (previousDrawResultId.equals(command.correctedDrawResultId())) {
            throw ProblemRest.conflict("draw.correct_result.same_result");
        }

        salesGuard.assertCanCorrectAppliedResult(
            draw.id(),
            command.correctedDrawResultId(),
            command.force()
        );

        var now = clock.instant();

        draw.overrideResult(
            command.correctedDrawResultId(),
            now,
            command.reason()
        );

        drawLifecyclePort.save(draw);

        var channel = drawChannelCatalog
            .findById(draw.tenantId(), draw.drawChannelId())
            .orElseThrow(() -> new IllegalStateException(
                "DrawChannel not found: " + draw.drawChannelId()));

        var event = new DrawResultCorrectedEvent(
            EventId.of(idGenerator.newUuid()),
            now,
            draw.tenantId(),
            draw.id(),
            draw.drawDate(),
            channel.resultSlotId(),
            previousDrawResultId,
            command.correctedDrawResultId(),
            draw.drawChannelId(),
            command.reason()
        );

        AfterCommit.run(() -> {
            if (!processedEventPort.markProcessedIfAbsent(HANDLER_KEY, idempotentEventId)) {
                log.warn("draw.correct_result duplicate skipped-after-commit drawId={} key={}",
                    command.drawId(), command.idempotencyKey());
                return;
            }
            eventPublisher.publish(event);
        });
    }
}



