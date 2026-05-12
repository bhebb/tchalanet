package com.tchalanet.server.core.draw.internal.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.api.command.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.internal.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.internal.application.query.projection.OpenableDrawRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDueDrawsCommandHandler
    implements CommandHandler<OpenDueDrawsCommand, OpenDueDrawsResult> {

    private static final int LOG_SAMPLE_IDS = 10;

    private final DrawLifecyclePort port;

    @Override
    @TchTx
    public OpenDueDrawsResult handle(OpenDueDrawsCommand command) {
        validateCommand(command);

        var openable =
            port.findOpenable(
                command.now(), command.batchSize(), command.lookaheadHours(), command.lagHours());

        int openableCount = openable.size();

        int skippedLocked = (int) openable.stream().filter(OpenableDrawRow::locked).count();

        var nonLockedIds =
            openable.stream().filter(r -> !r.locked()).map(OpenableDrawRow::drawId).toList();

        if (nonLockedIds.isEmpty()) {
            log.info(
                "draw.open_due no-op now={} batchSize={} lookaheadHours={} lagHours={} openable={} skippedLocked={}",
                command.now(),
                command.batchSize(),
                command.lookaheadHours(),
                command.lagHours(),
                openableCount,
                skippedLocked);

            return new OpenDueDrawsResult(0, skippedLocked, 0);
        }

        if (command.dryRun()) {
            log.info(
                "draw.open_due dryRun=true now={} batchSize={} lookaheadHours={} lagHours={} openable={} wouldOpen={} skippedLocked={} sampleIds={}",
                command.now(),
                command.batchSize(),
                command.lookaheadHours(),
                command.lagHours(),
                openableCount,
                nonLockedIds.size(),
                skippedLocked,
                sample(nonLockedIds));
            return new OpenDueDrawsResult(0, skippedLocked, 0);
        }

        int opened = port.bulkOpen(nonLockedIds, command.now());

        log.info(
            "draw.open_due now={} batchSize={} lookaheadHours={} lagHours={} openable={} opened={} skippedLocked={} sampleIds={}",
            command.now(),
            command.batchSize(),
            command.lookaheadHours(),
            command.lagHours(),
            openableCount,
            opened,
            skippedLocked,
            sample(nonLockedIds));

        return new OpenDueDrawsResult(opened, skippedLocked, 0);
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
