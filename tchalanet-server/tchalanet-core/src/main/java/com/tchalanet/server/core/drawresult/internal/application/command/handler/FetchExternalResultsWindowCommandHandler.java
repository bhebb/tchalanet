package com.tchalanet.server.core.drawresult.internal.application.command.handler;

import com.tchalanet.server.catalog.drawchannel.api.model.DrawSource;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotCatalog;
import com.tchalanet.server.catalog.resultslot.api.ResultSlotView;
import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.DateWindows;
import com.tchalanet.server.common.time.OccurredAtResolver;
import com.tchalanet.server.common.tx.AfterCommit;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowCommand;
import com.tchalanet.server.core.drawresult.api.command.FetchExternalResultsWindowResult;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultReaderPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.DrawResultWriterPort;
import com.tchalanet.server.core.drawresult.internal.application.port.out.notification.DrawResultFetchNotificationPort;
import com.tchalanet.server.core.drawresult.internal.application.service.DrawResultPersistPayload;
import com.tchalanet.server.core.drawresult.internal.application.service.DrawResultPersistenceAssembler;
import com.tchalanet.server.core.drawresult.internal.application.service.ExternalResultFetcher;
import com.tchalanet.server.core.drawresult.internal.application.service.FetchCounters;
import com.tchalanet.server.core.drawresult.internal.application.service.HaitiProjectionService;
import com.tchalanet.server.core.drawresult.internal.application.service.ResolvedExternalResults;
import com.tchalanet.server.core.drawresult.internal.application.service.ResultSlotSourceConfigResolver;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.internal.infra.config.DrawResultsProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
@RequiredArgsConstructor
public class FetchExternalResultsWindowCommandHandler
    implements CommandHandler<FetchExternalResultsWindowCommand, FetchExternalResultsWindowResult> {

    private final HaitiProjectionService haitiProjectionService;
    private final DrawResultPersistenceAssembler drawResultPersistenceAssembler;
    private final DrawResultWriterPort writer;
    private final DrawResultReaderPort drawResultReaderPort;
    private final ResultSlotCatalog resultSlotCatalog;
    private final DrawResultsProperties props;
    private final Clock clock;
    private final ExternalResultFetcher externalResultFetcher;
    private final ResultSlotSourceConfigResolver resultSlotSourceConfigResolver;
    private final DrawResultFetchNotificationPort drawResultFetchNotificationPort;

    @Override
    @TchTx
    public FetchExternalResultsWindowResult handle(FetchExternalResultsWindowCommand cmd) {
        validate(cmd);

        var daysBack = clampDaysBack(cmd.daysBack());
        var maxSlots = Math.min(cmd.maxSlots(), props.getLimits().getMaxSlotsPerTick());
        var dates = DateWindows.datesBackInclusive(cmd.baseDate(), daysBack);
        var now = Instant.now(clock);
        var counters = new FetchCounters();

        var fetchedNotifications =
            new ArrayList<DrawResultFetchNotificationPort.DrawResultFetchNotification>();

        var failureNotifications =
            new ArrayList<DrawResultFetchNotificationPort.DrawResultFetchFailure>();

        var slots = resolveSlots(cmd.slotKeys(), maxSlots, counters);

        for (var date : dates) {
            for (var slot : slots) {
                fetchOne(
                    cmd,
                    slot,
                    date,
                    now,
                    counters,
                    fetchedNotifications,
                    failureNotifications);
            }
        }

        if (!fetchedNotifications.isEmpty()) {
            var notifications = List.copyOf(fetchedNotifications);
            AfterCommit.run(() -> drawResultFetchNotificationPort.notifyFetchedBatch(notifications));
        }

        if (!failureNotifications.isEmpty()) {
            var notification =
                new DrawResultFetchNotificationPort.DrawResultFetchFailureBatchNotification(
                    cmd.baseDate(),
                    daysBack,
                    failureNotifications.size(),
                    List.copyOf(failureNotifications));

            AfterCommit.run(() -> drawResultFetchNotificationPort.notifyFetchFailedBatch(notification));
        }

        return counters.toResult();
    }

    private void fetchOne(
        FetchExternalResultsWindowCommand cmd,
        ResultSlotView slot,
        LocalDate date,
        Instant now,
        FetchCounters counters,
        List<DrawResultFetchNotificationPort.DrawResultFetchNotification> fetchedNotifications,
        List<DrawResultFetchNotificationPort.DrawResultFetchFailure> failureNotifications) {

        var expectedOccurredAt =
            OccurredAtResolver.resolveOrNow(
                null,
                date,
                slot.drawTime(),
                slot.timezone(),
                clock);

        try {
            var sourceCfg = resultSlotSourceConfigResolver.resolve(slot.sourceCfg());

            if (!sourceCfg.hasAnyActiveGame()) {
                counters.skipped++;
                return;
            }

            if (!cmd.force()
                && drawResultReaderPort.existsUsableExternalResult(slot.id(), date)) {
                counters.alreadyFetched++;
                log.debug(
                    "draw-results.fetch.skip already_usable slot={} date={} occurredAt={}",
                    slot.slotKey(),
                    date,
                    expectedOccurredAt);
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

            var projection = haitiProjectionService.project(slot, date, external);

            var payload =
                drawResultPersistenceAssembler.assemble(
                    slot,
                    date,
                    expectedOccurredAt,
                    external,
                    projection,
                    cmd.includeRaw());

            var upsert =
                writer.upsert(
                    slot.id(),
                    date,
                    expectedOccurredAt,
                    payload.sourceResult(),
                    payload.haitiResult(),
                    payload.rawPayload(),
                    DrawResultStatus.CONFIRMED.name(),
                    DrawSource.EXTERNAL.name(),
                    payload.flags(),
                    payload.quality(),
                    payload.sourceHash(),
                    cmd.reason(),
                    cmd.force());

            var changed = applyUpsertCounters(upsert, counters);

            if (changed) {
                fetchedNotifications.add(
                    buildDrawResultNotification(slot, date, expectedOccurredAt, external, payload));
            }

        } catch (Exception e) {
            counters.errors++;

            failureNotifications.add(
                new DrawResultFetchNotificationPort.DrawResultFetchFailure(
                    slot.provider(),
                    slot.slotKey(),
                    date,
                    expectedOccurredAt,
                    e.getClass().getSimpleName(),
                    e.getMessage()));

            log.warn(
                "draw-results.fetch failed slot={} date={} err={}",
                slot.slotKey(),
                date,
                e.getMessage(),
                e);
        }
    }

    private boolean applyUpsertCounters(DrawResultWriterPort.UpsertResult upsertResult, FetchCounters counters) {
        if (upsertResult == null) {
            counters.skipped++;
            return false;
        }

        if (upsertResult.id() == null) {
            counters.skipped++;
            return false;
        }

        if (upsertResult.created()) {
            counters.inserted++;
            return true;
        }

        if (upsertResult.updated()) {
            counters.updated++;
            return true;
        }

        if (upsertResult.skippedConfirmed()) {
            counters.skippedConfirmed++;
            return false;
        }

        if (upsertResult.skippedOverridden()) {
            counters.skippedOverridden++;
            return false;
        }

        counters.skipped++;
        return false;
    }

    private DrawResultFetchNotificationPort.DrawResultFetchNotification buildDrawResultNotification(
        ResultSlotView slot,
        LocalDate date,
        Instant occurredAt,
        ResolvedExternalResults external,
        DrawResultPersistPayload payload) {

        var gameCodes = new ArrayList<String>(2);

        if (external.hasPick3()
            && external.pick3().gameCode() != null
            && !external.pick3().gameCode().isBlank()) {
            gameCodes.add(external.pick3().gameCode());
        }

        if (external.hasPick4()
            && external.pick4().gameCode() != null
            && !external.pick4().gameCode().isBlank()) {
            gameCodes.add(external.pick4().gameCode());
        }

        var metadata = new LinkedHashMap<String, String>();
        metadata.put("sourceResult", payload.sourceResult() == null ? "" : payload.sourceResult().toString());
        metadata.put("haitiProjection", payload.haitiResult() == null ? "" : payload.haitiResult().toString());
        metadata.put("flags", payload.flags() == null ? "" : payload.flags().toString());
        metadata.put("sourceHash", payload.sourceHash() == null ? "" : payload.sourceHash());

        return new DrawResultFetchNotificationPort.DrawResultFetchNotification(
            slot.provider(),
            slot.slotKey(),
            date,
            occurredAt,
            DrawResultStatus.CONFIRMED.name(),
            payload.quality(),
            gameCodes.size(),
            List.copyOf(gameCodes),
            metadata);
    }

    private List<ResultSlotView> resolveSlots(
        List<String> rawSlotKeys,
        int maxSlots,
        FetchCounters counters) {

        var out = new ArrayList<ResultSlotView>();

        for (var key :
            rawSlotKeys.stream()
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
