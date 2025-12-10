package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.LoginAgentCommand;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class LoginAgentCommandHandler implements CommandHandler<LoginAgentCommand, UUID> {

  @Override
  public UUID handle(LoginAgentCommand command) {
    // TODO: validate PIN, create session, return session id
    throw new UnsupportedOperationException("LoginAgentCommandHandler not implemented yet");
  }
}

