package com.tchalanet.server.core.session.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.session.application.command.model.LockSessionAfterInactivityCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class LockSessionAfterInactivityCommandHandler implements VoidCommandHandler<LockSessionAfterInactivityCommand> {

  @Override
  public void handle(LockSessionAfterInactivityCommand command) {
    // TODO: mark session as locked
    throw new UnsupportedOperationException("LockSessionAfterInactivityCommandHandler not implemented yet");
  }
}

