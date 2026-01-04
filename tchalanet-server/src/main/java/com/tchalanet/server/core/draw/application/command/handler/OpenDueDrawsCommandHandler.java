package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDueDrawsCommandHandler
    implements CommandHandler<OpenDueDrawsCommand, OpenDueDrawsResult> {

  private static final int LOG_SAMPLE_IDS = 10;

  private final DrawLifecyclePort port;

  @Override
  public OpenDueDrawsResult handle(OpenDueDrawsCommand command) {
    validateCommand(command);

    var openable =
        port.findOpenable(
            command.now(), command.limit(), command.openHorizonHours(), command.openLagHours());

    int openableCount = openable.size();

    int skippedLocked = (int) openable.stream().filter(OpenableDrawRow::locked).count();
    var ids = openable.stream().filter(r -> !r.locked()).map(OpenableDrawRow::drawId).toList();

    if (command.dryRun()) {
      log.info(
          "draw.open_due dryRun=true now={} limit={} horizonHours={} lagHours={} openable={} wouldOpen={} skippedLocked={} sampleIds={}",
          command.now(),
          command.limit(),
          command.openHorizonHours(),
          command.openLagHours(),
          openableCount,
          ids.size(),
          skippedLocked,
          sample(ids));
      return new OpenDueDrawsResult(0, skippedLocked, 0);
    }

    int opened = port.bulkOpen(ids);

    log.info(
        "draw.open_due now={} limit={} horizonHours={} lagHours={} openable={} opened={} skippedLocked={} sampleIds={}",
        command.now(),
        command.limit(),
        command.openHorizonHours(),
        command.openLagHours(),
        openableCount,
        opened,
        skippedLocked,
        sample(ids));

    return new OpenDueDrawsResult(opened, skippedLocked, 0);
  }

  private static void validateCommand(OpenDueDrawsCommand command) {
    Objects.requireNonNull(command, "command is required");
    Objects.requireNonNull(command.now(), "now is required");
  }

  private static List<String> sample(List<?> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    return ids.stream().limit(LOG_SAMPLE_IDS).map(Object::toString).collect(Collectors.toList());
  }
}
