package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.application.port.out.DrawStorePort;
import java.util.Objects;

import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDueDrawsCommandHandler implements CommandHandler<CloseDueDrawsCommand, CloseDueDrawsResult> {

    private final DrawStorePort drawStorePort;

    public CloseDueDrawsResult handle(CloseDueDrawsCommand command) {
        validateCommand(command);

        var due = drawStorePort.findDueToClose(command.now(), command.limit());
        var skippedLocked = (int) due.stream().filter(DueToCloseRow::locked).count();
        var ids = due.stream().filter(row -> !row.locked()).map(row -> row.drawId()).toList();

        if (command.dryRun()) {
            log.info(
                "closeDueDraws(dryRun=true) now={} wouldClose={} skippedLocked={}",
                command.now(),
                ids.size(),
                skippedLocked);
            return new CloseDueDrawsResult(0, skippedLocked);
        }

        var closed = drawStorePort.bulkClose(ids);

        log.info(
            "closeDueDraws now={} closed={} skippedLocked={}",
            command.now(),
            closed,
            skippedLocked);

        return new CloseDueDrawsResult(closed, skippedLocked);
    }

    private static void validateCommand(CloseDueDrawsCommand command) {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(command.now(), "now is required");
        if (command.limit() <= 0) {
            throw new IllegalArgumentException("limit must be > 0");
        }
    }
}
