package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.OpenTodayDrawsCommand;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.ResultSlotCalendarReaderPort;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenTodayDrawsCommandHandler
    implements CommandHandler<OpenTodayDrawsCommand, OpenDueDrawsResult> {

    private static final int LOG_SAMPLE_IDS = 10;

    private static final String PROVIDER_CLOSED_REASON_CODE = "PROVIDER_CLOSED";
    private static final String PROVIDER_CLOSED_REASON_LABEL = "No provider draw for this slot on this date";

    private final DrawLifecyclePort port;
    private final ResultSlotCalendarReaderPort resultSlotCalendarReader;
    private final DrawProperties drawProperties;

    @Override
    @TchTx
    public OpenDueDrawsResult handle(OpenTodayDrawsCommand command) {
        validate(command);

        var defaultSalesOpenTime = drawProperties.getScheduler().getOpenToday().getDefaultSalesOpenTime();
        var openable = port.findOpenableForSalesOpenTime(
            command.now(),
            command.drawDate(),
            defaultSalesOpenTime,
            command.batchSize());
        int openableCount = openable.size();
        int skippedLocked = (int) openable.stream().filter(OpenableDrawRow::locked).count();
        var nonLocked = openable.stream().filter(r -> !r.locked()).toList();

        if (nonLocked.isEmpty()) {
            log.info(
                "draw.open_today no-op now={} drawDate={} batchSize={} openable={} skippedLocked={}",
                command.now(),
                command.drawDate(),
                command.batchSize(),
                openableCount,
                skippedLocked);
            return new OpenDueDrawsResult(0, skippedLocked, 0, 0);
        }

        // Provider calendar: a draw whose result_slot has no draw on its draw_date
        // must be CANCELED (PROVIDER_CLOSED), not opened. draw_date is the slot-local
        // date so no timezone conversion is needed here.
        var openIds = new ArrayList<DrawId>();
        var cancelIds = new ArrayList<DrawId>();
        Map<String, Boolean> unavailableCache = new HashMap<>();
        for (var row : nonLocked) {
            var slot = row.resultSlotId();
            var drawDate = row.drawDate();
            boolean unavailable = slot != null && drawDate != null
                && unavailableCache.computeIfAbsent(
                    slot.value() + "|" + drawDate,
                    k -> resultSlotCalendarReader
                        .findUnavailableDates(slot, drawDate, drawDate)
                        .contains(drawDate));
            (unavailable ? cancelIds : openIds).add(row.drawId());
        }

        if (command.dryRun()) {
            log.info(
                "draw.open_today dryRun=true now={} drawDate={} batchSize={} openable={} wouldOpen={} wouldCancelProviderClosed={} skippedLocked={} sampleIds={}",
                command.now(),
                command.drawDate(),
                command.batchSize(),
                openableCount,
                openIds.size(),
                cancelIds.size(),
                skippedLocked,
                sample(openIds));
            return new OpenDueDrawsResult(0, skippedLocked, 0, 0);
        }

        int canceledProviderClosed = cancelIds.isEmpty()
            ? 0
            : port.bulkCancelScheduled(
                cancelIds, PROVIDER_CLOSED_REASON_CODE, PROVIDER_CLOSED_REASON_LABEL, command.now());

        int opened = openIds.isEmpty() ? 0 : port.bulkOpen(openIds, command.now());

        log.info(
            "draw.open_today now={} drawDate={} batchSize={} openable={} opened={} canceledProviderClosed={} skippedLocked={} sampleIds={}",
            command.now(),
            command.drawDate(),
            command.batchSize(),
            openableCount,
            opened,
            canceledProviderClosed,
            skippedLocked,
            sample(openIds));

        return new OpenDueDrawsResult(opened, skippedLocked, 0, canceledProviderClosed);
    }

    private static void validate(OpenTodayDrawsCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.now(), "now is required");
        if (command.batchSize() <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
    }

    private static List<String> sample(List<?> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().limit(LOG_SAMPLE_IDS).map(Object::toString).collect(Collectors.toList());
    }
}
