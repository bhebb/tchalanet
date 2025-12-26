package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsResult;
import com.tchalanet.server.core.draw.application.port.out.DrawLifecyclePort;
import com.tchalanet.server.core.draw.application.query.projection.OpenableDrawRow;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;

@UseCase
@RequiredArgsConstructor
@Slf4j
@Validated
public class OpenDueDrawsCommandHandler
    implements CommandHandler<OpenDueDrawsCommand, OpenDueDrawsResult> {

  private final DrawLifecyclePort port;

  public OpenDueDrawsResult handle(@Valid OpenDueDrawsCommand command) {
    var openable =
        port.findOpenable(
            command.now(), command.limit(), command.openHorizonHours(), command.openLagHours());

    var skippedLocked = (int) openable.stream().filter(OpenableDrawRow::locked).count();
    var ids = openable.stream().filter(row -> !row.locked()).map(OpenableDrawRow::drawId).toList();

    if (command.dryRun()) {
      log.info(
          "openDueDraws(dryRun=true) now={} wouldOpen={} skippedLocked={}",
          command.now(),
          ids.size(),
          skippedLocked);
      return new OpenDueDrawsResult(0, skippedLocked, 0);
    }

    var opened = port.bulkOpen(ids);

    log.info(
        "openDueDraws now={} opened={} skippedLocked={}", command.now(), opened, skippedLocked);

    return new OpenDueDrawsResult(opened, skippedLocked, 0);
  }
}
