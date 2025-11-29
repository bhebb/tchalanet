package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RebuildDrawReadModelsCommand;
import com.tchalanet.server.core.draw.application.port.in.command.RebuildDrawReadModelsCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RebuildDrawReadModelsUseCase implements RebuildDrawReadModelsCommandHandler {

  @Override
  public void handle(RebuildDrawReadModelsCommand command) {
    log.info("RebuildDrawReadModelsUseCase.handle - placeholder for command={}", command);
    // TODO: reconstruire les read models (reprojection) via les ports out appropriés
  }
}
