package com.tchalanet.server.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.draw.application.command.model.InvalidateDrawResultCommand;
import com.tchalanet.server.draw.application.port.in.command.InvalidateDrawResultCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawResultUseCase implements InvalidateDrawResultCommandHandler {

  @Override
  public void handle(InvalidateDrawResultCommand command) {
    log.info("InvalidateDrawResultUseCase.handle - placeholder for command={}", command);
    // TODO: invalider/unset le résultat du tirage et recalculer si nécessaire
  }
}
