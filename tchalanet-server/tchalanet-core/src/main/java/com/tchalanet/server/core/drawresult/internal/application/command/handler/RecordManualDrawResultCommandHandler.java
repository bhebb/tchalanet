package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultResult;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.external.ExternalSourceFlags;
import com.tchalanet.server.core.haiti.api.HaitiProjectionOutput;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import com.tchalanet.server.core.haiti.internal.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.internal.domain.lottery.model.ExternalPick;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.node.ObjectNode;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RecordManualDrawResultCommandHandler
    implements CommandHandler<RecordManualDrawResultCommand, RecordManualDrawResultResult> {

    private final ResultSlotCatalog slotReader;
    private final DrawResultWriterPort writer;
    private final HaitiLotteryPort haitiLotteryPort;
    private final JsonUtils jsonUtils;
    private final HaitiProjectionConfigPort haitiProjectionConfigPort;

    @Override
    public RecordManualDrawResultResult handle(RecordManualDrawResultCommand command) {
        TenantId tenantId = command.tenantId();
        var slot = resolveSlot(command.slotKey());
        var occurredAt = resolveOccurredAt(command, slot);
        var sourceResult = buildSourceResult(command, slot, occurredAt);
        var flags = buildFlags(command);
        var haitiResult = projectHaiti(command, slot);

        var res =
            writer.upsert(
                slot.id(),
                command.drawDate(),
                occurredAt,
                sourceResult,
                jsonUtils.toJsonNode(haitiResult.result()),
                sourceResult,
                DrawResultStatus.CONFIRMED.name(),
                DrawSource.MANUAL.name(),
                flags,
                ResultQuality.COMPLETE.name(),
                null,
                command.notes(),
                command.force());

        log.info(
            "draw_result.manual tenant={} slotKey={} occurredAt={} resultId={} created={} updated={}",
            tenantId,
            slot.slotKey(),
            occurredAt,
            res.id(),
            res.created(),
            res.updated());

        return new RecordManualDrawResultResult(res.id(), res.created(), res.updated());
    }

    private ResultSlotView resolveSlot(String rawSlotKey) {
        var slotKey = normalizeSlotKey(rawSlotKey);

        return slotReader
            .findByKey(slotKey)
            .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));
    }

    private Instant resolveOccurredAt(
        RecordManualDrawResultCommand command,
        ResultSlotView slot) {
        return OccurredAtResolver.resolveOrThrow(
            null,
            command.drawDate(),
            slot.drawTime(),
            slot.timezone());
    }

    private ObjectNode buildSourceResult(
        RecordManualDrawResultCommand command,
        ResultSlotView slot,
        Instant occurredAt) {

        var sourceResult = jsonUtils.emptyObject();
        sourceResult.put("mode", "MANUAL");
        sourceResult.put("slot_key", slot.slotKey());
        sourceResult.put("provider", slot.provider());
        sourceResult.put("draw_date", command.drawDate().toString());
        sourceResult.put("occurred_at", occurredAt.toString());
        sourceResult.put("recorded_by", emptyIfNull(command.recordedBy()));
        sourceResult.put("notes", emptyIfNull(command.notes()));

        putIfNotBlank(sourceResult, "pick3", command.pick3());
        putIfNotBlank(sourceResult, "pick4", command.pick4());

        return sourceResult;
    }

    private tools.jackson.databind.JsonNode buildFlags(RecordManualDrawResultCommand command) {
        return jsonUtils.toJsonNode(ExternalSourceFlags.manual(emptyIfNull(command.recordedBy())));
    }

    private HaitiProjectionOutput projectHaiti(
        RecordManualDrawResultCommand command,
        ResultSlotView slot) {
        return haitiLotteryPort.projectResult(
            ExternalPick.of(command.pick3(), command.pick4()),
            haitiProjectionConfigPort.resolve(slot.projectionCfg()));
    }

    private static void putIfNotBlank(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value.trim());
        }
    }

    private static String normalizeSlotKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value.trim();
    }
}
