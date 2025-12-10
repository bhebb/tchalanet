package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.RebuildDrawReadModelsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class RebuildDrawReadModelsCommandHandler
    implements VoidCommandHandler<RebuildDrawReadModelsCommand> {

  @Override
  public void handle(RebuildDrawReadModelsCommand command) {
    log.info("RebuildDrawReadModelsCommandHandler.handle - placeholder for command={}", command);
    // TODO: reconstruire les read models (reprojection) via les ports out appropriés
  }
}
