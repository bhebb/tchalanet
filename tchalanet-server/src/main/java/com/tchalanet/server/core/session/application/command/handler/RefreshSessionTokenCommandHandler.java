package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.RefreshSessionTokenCommand;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class RefreshSessionTokenCommandHandler implements CommandHandler<RefreshSessionTokenCommand, Map<String, String>> {

  @Override
  public Map<String, String> handle(RefreshSessionTokenCommand command) {
    // TODO: validate refresh token, return new access token + optional refresh token
    throw new UnsupportedOperationException("RefreshSessionTokenCommandHandler not implemented yet");
  }
}

