package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.ForceSyncNowCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class ForceSyncNowCommandHandler implements VoidCommandHandler<ForceSyncNowCommand> {

  @Override
  public void handle(ForceSyncNowCommand command) {
    // TODO: trigger offline sync for device
    throw new UnsupportedOperationException("ForceSyncNowCommandHandler not implemented yet");
  }
}

