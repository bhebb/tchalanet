package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UnregisterPosDeviceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UnregisterPosDeviceCommandHandler implements VoidCommandHandler<UnregisterPosDeviceCommand> {

  @Override
  public void handle(UnregisterPosDeviceCommand command) {
    // TODO: unregister device
    throw new UnsupportedOperationException("UnregisterPosDeviceCommandHandler not implemented yet");
  }
}

