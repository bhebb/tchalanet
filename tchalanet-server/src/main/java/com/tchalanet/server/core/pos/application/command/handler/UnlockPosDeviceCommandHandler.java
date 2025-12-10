package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UnlockPosDeviceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UnlockPosDeviceCommandHandler implements VoidCommandHandler<UnlockPosDeviceCommand> {

  @Override
  public void handle(UnlockPosDeviceCommand command) {
    // TODO: unlock device
    throw new UnsupportedOperationException("UnlockPosDeviceCommandHandler not implemented yet");
  }
}

