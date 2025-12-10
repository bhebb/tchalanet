package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.KickOtherSessionsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class KickOtherSessionsCommandHandler implements VoidCommandHandler<KickOtherSessionsCommand> {

  @Override
  public void handle(KickOtherSessionsCommand command) {
    // TODO: invalidate other sessions for agent
    throw new UnsupportedOperationException("KickOtherSessionsCommandHandler not implemented yet");
  }
}

