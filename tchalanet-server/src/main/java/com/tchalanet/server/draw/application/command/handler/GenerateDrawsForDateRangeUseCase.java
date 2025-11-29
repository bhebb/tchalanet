package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.GenerateDrawsForDateRangeCommand;
import com.tchalanet.server.draw.application.port.in.command.GenerateDrawsForDateRangeCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GenerateDrawsForDateRangeUseCase implements GenerateDrawsForDateRangeCommandHandler {

  @Override
  public void handle(GenerateDrawsForDateRangeCommand command) {
    log.info("GenerateDrawsForDateRangeUseCase.handle - placeholder for command={}", command);
    // TODO: générer les tirages pour la plage de dates indiquée
  }
}
