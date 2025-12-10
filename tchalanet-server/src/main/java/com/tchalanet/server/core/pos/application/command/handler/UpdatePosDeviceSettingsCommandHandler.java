package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.UpdatePosDeviceSettingsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class UpdatePosDeviceSettingsCommandHandler implements VoidCommandHandler<UpdatePosDeviceSettingsCommand> {

  @Override
  public void handle(UpdatePosDeviceSettingsCommand command) {
    // TODO: update settings
    throw new UnsupportedOperationException("UpdatePosDeviceSettingsCommandHandler not implemented yet");
  }
}

