package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.types.enums.DrawSource;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.application.service.*;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class FetchExternalResultsWindowCommandHandler
    implements CommandHandler<FetchExternalResultsWindowCommand, FetchExternalResultsWindowResult> {

    private final HaitiProjectionService haitiProjectionService;
    private final DrawResultPersistenceAssembler drawResultPersistenceAssembler;
    private final DrawResultWriterPort writer;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultsProperties props;
    private final Clock clock;
    private final ExternalResultFetcher externalResultFetcher;
    private final ResultSlotSourceConfigResolver resultSlotSourceConfigResolver;

    @Override
    @TchTx
    public FetchExternalResultsWindowResult handle(FetchExternalResultsWindowCommand cmd) {
        validate(cmd);

        var daysBack = clampDaysBack(cmd.daysBack());
        var maxSlots = Math.min(cmd.maxSlots(), props.getLimits().getMaxSlotsPerTick());
        var dates = DateWindows.datesBackInclusive(cmd.baseDate(), daysBack);
        var now = Instant.now(clock);
        var counters = new FetchCounters();

        var slots = resolveSlots(cmd.slotKeys(), maxSlots, counters);

        for (var date : dates) {
            for (var slot : slots) {
                fetchOne(cmd, slot, date, now, counters);
            }
        }

        return counters.toResult();
    }

    private void fetchOne(
        FetchExternalResultsWindowCommand cmd,
        ResultSlotView slot,
        LocalDate date,
        Instant now,
        FetchCounters counters) {

        try {
            var sourceCfg = resultSlotSourceConfigResolver.resolve(slot.sourceCfg());

            if (!sourceCfg.hasAnyActiveGame()) {
                counters.skipped++;
                return;
            }

            var external = externalResultFetcher.fetch(cmd, slot, sourceCfg, date, now);

            if (!external.hasAnyResult()) {
                counters.noExternalResult++;
                return;
            }

            if (cmd.dryRun()) {
                counters.dryRunMatched++;
                return;
            }

            var occurredAt =
                OccurredAtResolver.resolveOrNow(
                    external.firstOccurredAt(),
                    date,
                    slot.drawTime(),
                    slot.timezone(),
                    clock);

            var projection = haitiProjectionService.project(slot, date, external);

            var payload =
                drawResultPersistenceAssembler.assemble(
                    slot,
                    date,
                    occurredAt,
                    external,
                    projection,
                    cmd.includeRaw());

            var upsert =
                writer.upsert(
                    slot.id(),
                    occurredAt,
                    payload.sourceResult(),
                    payload.haitiResult(),
                    payload.rawPayload(),
                    DrawResultStatus.PROVISIONAL.name(),
                    DrawSource.EXTERNAL.name(),
                    payload.flags(),
                    payload.quality(),
                    payload.sourceHash(),
                    cmd.reason(),
                    cmd.force());

            if (upsert == null || upsert.id() == null) {
                counters.skipped++;
            } else if (upsert.created()) {
                counters.inserted++;
            } else if (upsert.updated()) {
                counters.updated++;
            } else if (upsert.skippedConfirmed()) {
                counters.skippedConfirmed++;
            } else if (upsert.skippedOverridden()) {
                counters.skippedOverridden++;
            } else {
                counters.skipped++;
            }

        } catch (Exception e) {
            counters.errors++;
            log.warn(
                "draw-results.fetch failed slot={} date={} err={}",
                slot.slotKey(),
                date,
                e.getMessage(),
                e);
        }
    }

    private List<ResultSlotView> resolveSlots(
        List<String> rawSlotKeys,
        int maxSlots,
        FetchCounters counters
    ) {
        var out = new ArrayList<ResultSlotView>();

        for (var key : rawSlotKeys.stream()
            .map(FetchExternalResultsWindowCommandHandler::normalizeKey)
            .filter(normalizedKey -> !normalizedKey.isBlank())
            .distinct()
            .limit(maxSlots)
            .toList()) {
            var slotOpt = resultSlotCatalog.findByKey(key);
            if (slotOpt.isEmpty()) {
                counters.slotNotFound++;
                continue;
            }
            var slot = slotOpt.get();
            if (!slot.active()) {
                counters.slotInactive++;
                continue;
            }
            out.add(slot);
        }

        return out;
    }


    private int clampDaysBack(int requestedDays) {
        int clamped = Math.max(0, requestedDays);
        return Math.min(clamped, props.getLimits().getHardDaysBack());
    }

    private static void validate(FetchExternalResultsWindowCommand cmd) {
        Objects.requireNonNull(cmd, "command is required");
        Objects.requireNonNull(cmd.baseDate(), "baseDate is required");

        if (cmd.daysBack() < 0) {
            throw new IllegalArgumentException("daysBack must be >= 0");
        }

        if (cmd.maxSlots() <= 0) {
            throw new IllegalArgumentException("maxSlots must be > 0");
        }
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(java.util.Locale.ROOT);
    }

}

