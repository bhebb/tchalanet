package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.InvalidateDrawResultCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class InvalidateDrawResultCommandHandler
    implements VoidCommandHandler<InvalidateDrawResultCommand> {

  @Override
  public void handle(InvalidateDrawResultCommand command) {
    log.info("InvalidateDrawResultCommandHandler.handle - placeholder for command={}", command);
    // TODO: invalider/unset le résultat du tirage et recalculer si nécessaire
  }
}
