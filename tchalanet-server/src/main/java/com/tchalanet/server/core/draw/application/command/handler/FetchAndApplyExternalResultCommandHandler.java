package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.draw.application.command.model.FetchAndApplyExternalResultCommand;
import com.tchalanet.server.core.draw.application.port.out.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.draw.application.port.out.DrawWriterPort;
import com.tchalanet.server.core.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultedEvent;
import com.tchalanet.server.core.draw.domain.model.DrawResult;
import com.tchalanet.server.core.draw.domain.model.DrawSource;
import com.tchalanet.server.core.tenant.domain.model.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;

/**
 * Use case pour récupérer les résultats d’un tirage via les providers externes / saisies manuelles
 * et les enregistrer.
 * <p>
 * implémenter la logique métier complète.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class FetchAndApplyExternalResultCommandHandler
    implements VoidCommandHandler<FetchAndApplyExternalResultCommand> {

    private final DrawReaderPort drawReaderPort;
    private final DrawWriterPort drawWriterPort;
    private final DrawResultWriterPort drawResultWriterPort;
    private final ExternalDrawResultPort externalDrawResultPort;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    @Override
    @TchTx
    public void handle(FetchAndApplyExternalResultCommand command) {
        var drawId = command.drawId();

        var draw =
            drawReaderPort
                .findById(drawId)
                .orElseThrow(() -> new IllegalArgumentException("Draw not found: " + drawId));

        var query =
            new ExternalDrawResultPort.DrawExternalQuery(
                draw.drawChannel().code(), draw.scheduledAt().toLocalDate(), draw.cutoffAt().toInstant(), command.force());

        var external = externalDrawResultPort.fetchExternalResult(query);

        // Si aucun résultat (numbers null ou status != FOUND), on ne fait rien
        if (!external.found() || external.numbers() == null) {
            log.info(
                "No external result: drawId={} status={} channel={} date={}",
                drawId,
                external.status(),
                draw.drawChannel().code(),
                external.occurredAt());
            return;
        }

        var result =
            new DrawResult(
                DrawSource.EXTERNAL,
                external.numbers(),
                external.numbersExtra(),
                external.occurredAt() != null ? external.occurredAt() : Instant.now(),
                external.rawPayload().toString(),
                false,
                null);

        var applied = false;
        if (draw.status() != com.tchalanet.server.core.draw.domain.model.DrawStatus.RESULTED || draw.result() == null) {
            draw.applyResult(result);
            applied = true;
        }

        drawResultWriterPort.save(draw.tenantId(), drawId, result);
        drawWriterPort.save(draw);

        if (applied) {
            var resultPayloadJson = String.format(
                "{\"numbers_main\":%s,\"numbers_extra\":%s,\"source\":\"%s\",\"occurred_at\":\"%s\",\"override\":false}",
                result.numbersMain(),
                result.numbersExtra(),
                result.source(),
                result.occurredAt()
            );
            var event = new DrawResultedEvent(
                java.util.UUID.randomUUID(),
                Instant.now(clock),
                TenantId.of(draw.tenantId()),
                drawId,
                draw.drawChannel().code(),
                draw.scheduledAt().toInstant(),
                draw.drawChannel().code(), // TODO: use proper channelCode
                resultPayloadJson
            );
            AfterCommit.run(() -> publisher.publish(event));
        }
    }
}
