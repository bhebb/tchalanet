package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.CloseDueDrawsResult;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class CloseDueDrawsCommandHandler
    implements CommandHandler<CloseDueDrawsCommand, CloseDueDrawsResult> {

  private static final int LOG_SAMPLE_IDS = 10;

  private final DrawLifecyclePort drawLifecyclePort;

  @Override
  @TchTx
  public CloseDueDrawsResult handle(CloseDueDrawsCommand command) {
    validateCommand(command);

    var due = drawLifecyclePort.findDueToClose(command.now(), command.batchSize());
    int dueCount = due.size();

    int skippedLocked = (int) due.stream().filter(DueToCloseRow::locked).count();
    var ids = due.stream().filter(r -> !r.locked()).map(DueToCloseRow::drawId).toList();

    if (command.dryRun()) {
      log.info(
          "draw.close_due dryRun=true now={} batchSize={} due={} wouldClose={} skippedLocked={} sampleIds={}",
          command.now(),
          command.batchSize(),
          dueCount,
          ids.size(),
          skippedLocked,
          sample(ids));
      return new CloseDueDrawsResult(0, skippedLocked);
    }

    int closed = drawLifecyclePort.bulkClose(ids);

    log.info(
        "draw.close_due now={} batchSize={} due={} closed={} skippedLocked={} sampleIds={}",
        command.now(),
        command.batchSize(),
        dueCount,
        closed,
        skippedLocked,
        sample(ids));

    return new CloseDueDrawsResult(closed, skippedLocked);
  }

  private static void validateCommand(CloseDueDrawsCommand command) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(command.now(), "now is required");
    if (command.batchSize() <= 0) throw new IllegalArgumentException("batchSize must be > 0");
  }

  private static List<String> sample(List<?> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return ids.stream().limit(LOG_SAMPLE_IDS).map(Object::toString).collect(Collectors.toList());
  }
}
