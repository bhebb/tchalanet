package com.tchalanet.server.core.draw.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.draw.application.command.model.OpenDueDrawsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class OpenDueDrawsCommandHandler implements VoidCommandHandler<OpenDueDrawsCommand> {

  @Override
  public void handle(OpenDueDrawsCommand command) {
    log.info("OpenDueDrawsCommandHandler.handle - placeholder for command={}", command);
    // TODO: ouvrir les tirages dus pour le tenant / plage fournie
  }
}
