package com.tchalanet.server.core.drawresult.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.event.DomainEventPublisher;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.id.EventId;
import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.draw.api.DrawReaderPort;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.port.out.DrawLookupPort;
import com.tchalanet.server.core.draw.domain.event.DrawResultAppliedEvent;
import com.tchalanet.server.core.draw.domain.model.DrawStatus;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.OverrideDrawResultResult;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.haiti.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.drawresult.domain.exception.DrawResultOverrideForbiddenException;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.haiti.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OverrideDrawResultCommandHandler
    implements CommandHandler<OverrideDrawResultCommand, OverrideDrawResultResult> {

    private final ResultSlotCatalog slotReader;
    private final DrawResultWriterPort writer;
    private final DrawResultReaderPort resultReader;
    private final DrawReaderPort drawReader;
    private final DrawLookupPort drawLookup;
    private final DrawLifecyclePort drawWriter;
    private final DomainEventPublisher publisher;
    private final IdGenerator idGenerator;
    private final JsonUtils jsonUtils;
    private final Clock clock;
    private final HaitiProjectionConfigPort haitiProjectionConfigPort;
    private final HaitiLotteryPort haitiPort;


    @Override
    public OverrideDrawResultResult handle(OverrideDrawResultCommand command) {
        String slotKey = normalizeSlotKey(command.slotKey());

        var slot =
            slotReader
                .findByKey(slotKey)
                .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));

        // [D7] Use OccurredAtResolver instead of ResultSlotTimes
        Instant occurredAt = OccurredAtResolver.resolve(
            null, command.drawDate(), slot.drawTime(), slot.timezone(), clock);

        // [Règle Override refusé post-SETTLED]
        resultReader.findByResultSlotIdAndOccurredAt(slot.id(), occurredAt).ifPresent(drId -> {
            if (drawReader.existsSettledDrawForResult(drId)) {
                throw new DrawResultOverrideForbiddenException(drId);
            }
        });

        // source_result (canonique, même pour un override)
        ObjectNode sourceResult = buildSourceResult(command, slot, occurredAt);

        // flags
        var flags = jsonUtils.toJsonNode(ExternalSourceFlags.override(command.reason()));

        // [D10] Use enum.name()
        var haitiResult =
            haitiPort.projectResult(
                ExternalPick.of(command.pick3(), command.pick4()),
                haitiProjectionConfigPort.resolve(slot.projectionCfg()));

        var res =
            writer.upsert(
                slot.id(),
                occurredAt,
                sourceResult,
                jsonUtils.toJsonNode(haitiResult.result()),
                sourceResult,
                DrawResultStatus.OVERRIDDEN.name(),
                DrawSource.ADMIN_OVERRIDE.name(),
                flags,
                ResultQuality.COMPLETE.name(),
                null,
                command.reason(),
                command.force());

        // [Re-apply après override valide]
        applyOverrideToDraws(res, slot);

        log.info(
            "draw_result.override slotKey={} occurredAt={} drawResultId={} created={} updated={}",
            slot.slotKey(),
            occurredAt,
            res.id(),
            res.created(),
            res.updated());

        return new OverrideDrawResultResult(res.id(), res.created(), res.updated());
    }

    private void applyOverrideToDraws(DrawResultWriterPort.UpsertResult res, ResultSlotView slot) {
        var draws = drawReader.findByDrawResultId(res.id());

        for (var summary : draws) {
            if (summary.status() == DrawStatus.SETTLED) {
                continue;
            }

            var now = clock.instant();
            var draw = drawLookup.findById(summary.id()).orElseThrow();

            draw.applyResult(res.id(), now, DrawSource.ADMIN_OVERRIDE);
            drawWriter.save(draw);

            var event =
                new DrawResultAppliedEvent(
                    EventId.of(idGenerator.newUuid()),
                    now,
                    draw.tenantId(),
                    draw.id(),
                    slot.id(),
                    res.id());

            AfterCommit.run(() -> publisher.publish(event));
        }
    }

    private @NonNull ObjectNode buildSourceResult(OverrideDrawResultCommand command, ResultSlotView slot, Instant occurredAt) {
        ObjectNode sourceResult = jsonUtils.emptyObject();
        sourceResult.put("mode", "OVERRIDE");
        sourceResult.put("actor", "OPS");
        sourceResult.put("slot_key", slot.slotKey());
        sourceResult.put("provider", slot.provider());
        sourceResult.put("draw_date", command.drawDate().toString());
        sourceResult.put("occurred_at", occurredAt.toString());
        if (command.pick3() != null && !command.pick3().isBlank())
            sourceResult.put("pick3", command.pick3().trim());
        if (command.pick4() != null && !command.pick4().isBlank())
            sourceResult.put("pick4", command.pick4().trim());
        if (command.reason() != null && !command.reason().isBlank())
            sourceResult.put("reason", command.reason().trim());
        return sourceResult;
    }

    private static String normalizeSlotKey(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
