package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import com.tchalanet.server.core.draw.application.port.in.command.OpenDueDrawsCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDueDrawsUseCase implements OpenDueDrawsCommandHandler {

  @Override
  public void handle(OpenDueDrawsCommand command) {
    log.info("OpenDueDrawsUseCase.handle - placeholder for command={}", command);
    // TODO: ouvrir les tirages dus pour le tenant / plage fournie
  }
}
