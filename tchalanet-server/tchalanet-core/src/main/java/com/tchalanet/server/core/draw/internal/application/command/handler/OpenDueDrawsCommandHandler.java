package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.port.out.ResultSlotCalendarReaderPort;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import com.tchalanet.server.common.types.id.DrawId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDueDrawsCommandHandler
    implements CommandHandler<OpenDueDrawsCommand, OpenDueDrawsResult> {

    private static final int LOG_SAMPLE_IDS = 10;
    private static final String PROVIDER_CLOSED_REASON_CODE = "PROVIDER_CLOSED";
    private static final String PROVIDER_CLOSED_REASON_LABEL = "No provider draw for this slot on this date";

    private final DrawLifecyclePort port;
    private final ResultSlotCalendarReaderPort resultSlotCalendarReader;

    @Override
    @TchTx
    public OpenDueDrawsResult handle(OpenDueDrawsCommand command) {
        validateCommand(command);

        var openable =
            port.findOpenable(
                command.now(), command.batchSize(), command.lookaheadHours(), command.lagHours());

        int openableCount = openable.size();

        int skippedLocked = (int) openable.stream().filter(OpenableDrawRow::locked).count();

        var nonLocked = openable.stream().filter(r -> !r.locked()).toList();

        if (nonLocked.isEmpty()) {
            log.info(
                "draw.open_due no-op now={} batchSize={} lookaheadHours={} lagHours={} openable={} skippedLocked={}",
                command.now(),
                command.batchSize(),
                command.lookaheadHours(),
                command.lagHours(),
                openableCount,
                skippedLocked);

            return new OpenDueDrawsResult(0, skippedLocked, 0, 0);
        }

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
                "draw.open_due dryRun=true now={} batchSize={} lookaheadHours={} lagHours={} openable={} wouldOpen={} wouldCancelProviderClosed={} skippedLocked={} sampleIds={}",
                command.now(),
                command.batchSize(),
                command.lookaheadHours(),
                command.lagHours(),
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
            "draw.open_due now={} batchSize={} lookaheadHours={} lagHours={} openable={} opened={} canceledProviderClosed={} skippedLocked={} sampleIds={}",
            command.now(),
            command.batchSize(),
            command.lookaheadHours(),
            command.lagHours(),
            openableCount,
            opened,
            canceledProviderClosed,
            skippedLocked,
            sample(openIds));

        return new OpenDueDrawsResult(opened, skippedLocked, 0, canceledProviderClosed);
    }

    private static void validateCommand(OpenDueDrawsCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.now(), "now is required");

        if (command.batchSize() <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }

        if (command.lookaheadHours() < 0) {
            throw new IllegalArgumentException("lookaheadHours must be >= 0");
        }

        if (command.lagHours() < 0) {
            throw new IllegalArgumentException("lagHours must be >= 0");
        }
    }

    private static List<String> sample(List<?> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream().limit(LOG_SAMPLE_IDS).map(Object::toString).collect(Collectors.toList());
    }
}
