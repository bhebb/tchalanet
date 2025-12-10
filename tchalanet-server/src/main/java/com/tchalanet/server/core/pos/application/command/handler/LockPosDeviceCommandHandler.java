package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.LockPosDeviceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class LockPosDeviceCommandHandler implements VoidCommandHandler<LockPosDeviceCommand> {

  @Override
  public void handle(LockPosDeviceCommand command) {
    // TODO: lock device (business rules)
    throw new UnsupportedOperationException("LockPosDeviceCommandHandler not implemented yet");
  }
}

