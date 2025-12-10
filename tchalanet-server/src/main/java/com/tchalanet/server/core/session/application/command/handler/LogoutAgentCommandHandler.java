package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.LogoutAgentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class LogoutAgentCommandHandler implements VoidCommandHandler<LogoutAgentCommand> {

  @Override
  public void handle(LogoutAgentCommand command) {
    // TODO: invalidate session
    throw new UnsupportedOperationException("LogoutAgentCommandHandler not implemented yet");
  }
}

