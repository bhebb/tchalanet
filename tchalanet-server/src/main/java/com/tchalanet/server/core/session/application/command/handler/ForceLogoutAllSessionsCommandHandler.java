package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.ForceLogoutAllSessionsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ForceLogoutAllSessionsCommandHandler implements VoidCommandHandler<ForceLogoutAllSessionsCommand> {

  @Override
  public void handle(ForceLogoutAllSessionsCommand command) {
    // TODO: force logout across sessions
    throw new UnsupportedOperationException("ForceLogoutAllSessionsCommandHandler not implemented yet");
  }
}

