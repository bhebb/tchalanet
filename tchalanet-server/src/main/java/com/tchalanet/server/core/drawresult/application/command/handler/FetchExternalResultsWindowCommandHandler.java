package com.tchalanet.server.core.drawresult.application.command.handler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.config.draw.DrawResultsCommonProperties;
import com.tchalanet.server.common.contracts.haiti.HaitiFlags;
import com.tchalanet.server.common.contracts.haiti.HaitiProjectionOutput;
import com.tchalanet.server.common.contracts.results.ExternalResultOutput;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.application.command.model.FetchExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.application.port.out.ExternalResultsFetchPort;
import com.tchalanet.server.core.drawresult.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.application.port.out.HaitiProjectionConfigPort;
import com.tchalanet.server.core.drawresult.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.infra.util.SourceResultBuilder;
import com.tchalanet.server.core.haiti.application.port.out.HaitiLotteryPort;
import com.tchalanet.server.core.haiti.domain.lottery.model.ExternalPick;
import com.tchalanet.server.core.haiti.domain.lottery.exception.InvalidExternalPickException;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class FetchExternalResultsWindowCommandHandler
    implements CommandHandler<FetchExternalResultsWindowCommand, FetchExternalResultsWindowResult> {

    private final ExternalResultsFetchPort fetchPort;
    private final HaitiLotteryPort haitiPort;
    private final HaitiProjectionConfigPort haitiConfigPort;
    private final DrawResultWriterPort writer;
    private final ResultSlotCatalog resultSlotCatalog;
    private final JsonUtils jsonUtils;
    private final DrawResultsCommonProperties props;
    private final Clock clock;
    private final MeterRegistry meterRegistry; // [D11] Prometheus metrics

    @Override
    public FetchExternalResultsWindowResult handle(FetchExternalResultsWindowCommand cmd) {
        validate(cmd);

        int inserted = 0, updated = 0, skipped = 0, notFound = 0, errors = 0;
        int slotNotFound = 0, slotInactive = 0, noExternalResult = 0;

        int daysBack = clampDaysBack(cmd.daysBack());
        List<LocalDate> dates = DateWindows.datesBackInclusive(cmd.baseDate(), daysBack);
        var projCfg = haitiConfigPort.getDefault();

        var slotKeys = cmd.slotKeys().stream()
            .map(FetchExternalResultsWindowCommandHandler::normalizeKey)
            .filter(s -> !s.isBlank())
            .distinct()
            .limit(cmd.maxSlots())
            .toList();

        Instant now = Instant.now(clock);

        for (var date : dates) {
            for (var slotKey : slotKeys) {
                try {
                    var slotOpt = resultSlotCatalog.findByKey(slotKey);
                    if (slotOpt.isEmpty()) {
                        slotNotFound++;
                        notFound++;
                        continue;
                    }

                    var slot = slotOpt.get();
                    if (!slot.active()) {
                        slotInactive++;
                        notFound++;
                        continue;
                    }

                    var q = new ExternalResultsFetchPort.ResultSlotFetchQuery(
                        slot.slotKey(), date, cmd.force(), cmd.dryRun(), now);

                    var bundle = fetchPort.fetchSlot(q);

                    ExternalResultOutput p3 = bundle == null ? null : bundle.pick3();
                    ExternalResultOutput p4 = bundle == null ? null : bundle.pick4();

                    boolean anyFound =
                        (p3 != null && p3.found() && p3.main() != null && !p3.main().isEmpty())
                            || (p4 != null && p4.found() && p4.main() != null && !p4.main().isEmpty());

                    if (!anyFound) {
                        noExternalResult++;
                        notFound++;
                        continue;
                    }

                    if (cmd.dryRun()) {
                        skipped++;
                        continue;
                    }

                    var occurredAt = OccurredAtResolver.resolve(
                        (p3 != null ? p3.occurredAt() : (p4 != null ? p4.occurredAt() : null)),
                        date,
                        slot.drawTime(),
                        slot.timezone(),
                        clock);

                    var sourceResult = SourceResultBuilder.build(
                        jsonUtils, slot.provider(), slot.slotKey(), date, occurredAt, p3, p4);

                    ObjectNode haitiResultNode;
                    HaitiFlags haitiFlags;

                    try {
                        String pick3 = (p3 != null && p3.found() && p3.main() != null && !p3.main().isEmpty()) ? String.join("", p3.main()) : "";
                        String pick4 = (p4 != null && p4.found() && p4.main() != null && !p4.main().isEmpty()) ? String.join("", p4.main()) : "";

                        var pick = ExternalPick.of(pick3, pick4);
                        HaitiProjectionOutput hp = haitiPort.projectResult(pick, projCfg);

                        haitiResultNode = coerceHaitiLots(hp == null ? null : hp.result());
                        haitiFlags = hp == null ? HaitiFlags.fail(1, "PROJECTION_NULL", Map.of()) : hp.flags();

                    } catch (InvalidExternalPickException e) {
                        log.warn("draw-results.fetch invalid_pick slot={} date={} err={}", slot.slotKey(), date, e.getMessage());
                        // [D11] Increment Prometheus counter
                        meterRegistry.counter("draw_external_pick_invalid_total", "slot", slot.slotKey()).increment();
                        skipped++;
                        continue;
                    } catch (Exception e) {
                        log.warn("draw-results.fetch projection_failed slot={} date={} err={}", slot.slotKey(), date, e.toString());
                        haitiResultNode = emptyHaitiLots();
                        haitiFlags = HaitiFlags.fail(1, "PROJECTION_EXCEPTION", Map.of("error", String.valueOf(e.getMessage())));
                    }

                    var flags = jsonUtils.emptyObjectNode();
                    var sourceFlags = jsonUtils.emptyObjectNode();
                    if (p3 != null) sourceFlags.set("pick3", jsonUtils.valueToTree(p3.sourceFlags()));
                    if (p4 != null) sourceFlags.set("pick4", jsonUtils.valueToTree(p4.sourceFlags()));
                    flags.set("source", sourceFlags);
                    flags.set("haiti", jsonUtils.valueToTree(haitiFlags));

                    var raw = jsonUtils.emptyObjectNode();
                    if (p3 != null) raw.set("pick3_raw", jsonUtils.valueToTree(p3.rawPayload()));
                    if (p4 != null) raw.set("pick4_raw", jsonUtils.valueToTree(p4.rawPayload()));
                    if (bundle != null && bundle.raw() != null) {
                        raw.set("provider_payload", jsonUtils.valueToTree(bundle.raw()));
                    }

                    var chosenQuality = (p3 != null && p3.found()) ? p3.quality() : (p4 != null && p4.found() ? p4.quality() : null);
                    String quality = chosenQuality == null ? null : chosenQuality.name();
                    String sourceHash = (p3 != null && p3.found()) ? p3.sourceFlags().hash() : (p4 != null && p4.found() ? p4.sourceFlags().hash() : null);

                    var up = writer.upsert(
                        slot.id(),
                        occurredAt,
                        sourceResult,
                        haitiResultNode,
                        raw,
                        DrawResultStatus.PROVISIONAL.name(),
                        DrawSource.EXTERNAL.name(),
                        flags,
                        quality,
                        sourceHash,
                        null,
                        cmd.force());

                    if (up == null || up.id() == null) {
                        skipped++;
                        continue;
                    }

                    if (up.created()) inserted++;
                    else if (up.updated()) updated++;
                    else skipped++;

                } catch (Exception e) {
                    errors++;
                    log.warn("draw-results.fetch failed slot={} date={} err={}", slotKey, date, e.toString());
                }
            }
        }

        return new FetchExternalResultsWindowResult(inserted, updated, errors, skipped, notFound);
    }

    private int clampDaysBack(int v) {
        int x = Math.max(0, v);
        return Math.min(x, props.getLimits().getHardDaysBack());
    }

    private static void validate(FetchExternalResultsWindowCommand cmd) {
        Objects.requireNonNull(cmd, "command is required");
        Objects.requireNonNull(cmd.baseDate(), "baseDate is required");
        if (cmd.daysBack() < 0) throw new IllegalArgumentException("daysBack must be >= 0");
        if (cmd.slotKeys() == null || cmd.slotKeys().isEmpty()) throw new IllegalArgumentException("slotKeys required");
        if (cmd.maxSlots() <= 0) throw new IllegalArgumentException("maxSlots must be > 0");
    }

    private static String normalizeKey(String s) {
        return s == null ? "" : s.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private ObjectNode coerceHaitiLots(Object anyResult) {
        if (anyResult == null) return emptyHaitiLots();
        var node = jsonUtils.valueToTree(anyResult);
        if (node == null || node.isNull() || !node.isObject()) return emptyHaitiLots();
        var obj = (ObjectNode) node;
        if (!obj.has("lot1")) obj.put("lot1", "");
        if (!obj.has("lot2")) obj.put("lot2", "");
        if (!obj.has("lot3")) obj.put("lot3", "");
        if (!obj.has("lot4")) obj.put("lot4", "");
        return obj;
    }

    private ObjectNode emptyHaitiLots() {
        var o = jsonUtils.emptyObjectNode();
        o.put("lot1", "");
        o.put("lot2", "");
        o.put("lot3", "");
        o.put("lot4", "");
        return o;
    }
}
