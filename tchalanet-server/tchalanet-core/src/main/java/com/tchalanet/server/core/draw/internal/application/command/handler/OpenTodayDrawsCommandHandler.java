package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.api.command.OpenTodayDrawsCommand;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.infra.config.DrawProperties;
import com.tchalanet.server.core.draw.api.query.OpenableDrawRow;
import java.util.List;
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

    private final DrawLifecyclePort port;
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
        var nonLockedIds = openable.stream()
            .filter(r -> !r.locked())
            .map(OpenableDrawRow::drawId)
            .toList();

        if (nonLockedIds.isEmpty()) {
            log.info(
                "draw.open_today no-op now={} drawDate={} batchSize={} openable={} skippedLocked={}",
                command.now(),
                command.drawDate(),
                command.batchSize(),
                openableCount,
                skippedLocked);
            return new OpenDueDrawsResult(0, skippedLocked, 0);
        }

        if (command.dryRun()) {
            log.info(
                "draw.open_today dryRun=true now={} drawDate={} batchSize={} openable={} wouldOpen={} skippedLocked={} sampleIds={}",
                command.now(),
                command.drawDate(),
                command.batchSize(),
                openableCount,
                nonLockedIds.size(),
                skippedLocked,
                sample(nonLockedIds));
            return new OpenDueDrawsResult(0, skippedLocked, 0);
        }

        int opened = port.bulkOpen(nonLockedIds, command.now());
        log.info(
            "draw.open_today now={} drawDate={} batchSize={} openable={} opened={} skippedLocked={} sampleIds={}",
            command.now(),
            command.drawDate(),
            command.batchSize(),
            openableCount,
            opened,
            skippedLocked,
            sample(nonLockedIds));

        return new OpenDueDrawsResult(opened, skippedLocked, 0);
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
