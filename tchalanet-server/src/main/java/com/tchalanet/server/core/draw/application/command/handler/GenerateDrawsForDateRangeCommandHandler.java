package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.GenerateDrawsForDateRangeCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForDateRangeCommandHandler
    implements VoidCommandHandler<GenerateDrawsForDateRangeCommand> {

  @Override
  public void handle(GenerateDrawsForDateRangeCommand command) {
    log.info(
        "GenerateDrawsForDateRangeCommandHandler.handle - placeholder for command={}", command);
    // TODO: générer les tirages pour la plage de dates indiquée
  }
}
