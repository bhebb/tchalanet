package com.tchalanet.server.core.drawresult.application.command.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.contracts.results.SourceFlags;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.common.types.enums.ResultQuality;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.drawresult.application.command.model.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.application.command.model.RecordManualDrawResultResult;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.haiti.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final Clock clock;

    @Override
    public RecordManualDrawResultResult handle(RecordManualDrawResultCommand command) {
        TenantId tenantId = command.tenantId();

        String slotKey = normalizeSlotKey(command.slotKey());
        var slot =
            slotReader
                .findByKey(slotKey)
                .orElseThrow(() -> new IllegalArgumentException("result_slot not found: " + slotKey));

        // [D7] Use OccurredAtResolver instead of ResultSlotTimes
        Instant occurredAt = OccurredAtResolver.resolve(
            null, command.drawDate(), slot.drawTime(), slot.timezone(), clock);

        // source_result (audit)
        ObjectNode sourceResult = jsonUtils.emptyObjectNode();
        sourceResult.put("mode", "MANUAL");
        sourceResult.put("slot_key", slot.slotKey());
        sourceResult.put("provider", slot.provider());
        sourceResult.put("draw_date", command.drawDate().toString());
        sourceResult.put("occurred_at", occurredAt.toString());
        sourceResult.put("recorded_by", nn(command.recordedBy() != null ? command.recordedBy().toString() : ""));
        sourceResult.put("notes", nn(command.notes()));
        if (command.pick3() != null && !command.pick3().isBlank())
            sourceResult.put("pick3", command.pick3().trim());
        if (command.pick4() != null && !command.pick4().isBlank())
            sourceResult.put("pick4", command.pick4().trim());

        // flags
        var flags = jsonUtils.valueToTree(SourceFlags.manual("MANUAL", command.recordedBy() != null ? command.recordedBy().toString() : ""));

        // [D11] ExternalPick.of() validation
        var haitiResult =
            haitiLotteryPort.projectResult(
                ExternalPick.of(command.pick3(), command.pick4()),
                haitiProjectionConfigPort.getDefault());

        // [D10] Use enum.name()
        String status = DrawResultStatus.FINAL.name();
        String source = DrawSource.MANUAL.name();
        String quality = ResultQuality.COMPLETE.name();

        var res =
            writer.upsert(
                slot.id(),
                occurredAt,
                sourceResult,
                jsonUtils.valueToTree(haitiResult.result()),
                sourceResult,
                status,
                source,
                flags,
                quality,
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

    private static String normalizeSlotKey(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static String nn(String s) {
        return s == null ? "" : s.trim();
    }
}
