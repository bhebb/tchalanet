package com.tchalanet.server.core.pos.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.pos.application.command.model.RegisterPosDeviceCommand;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class RegisterPosDeviceCommandHandler implements CommandHandler<RegisterPosDeviceCommand, UUID> {

  @Override
  public UUID handle(RegisterPosDeviceCommand command) {
    // TODO: persist device registration and return deviceId
    throw new UnsupportedOperationException("RegisterPosDeviceCommandHandler not implemented yet");
  }
}

