package com.tchalanet.server.core.offlinesync.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.application.command.model.RestoreOfflineDataOnNewDeviceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@UseCase
@RequiredArgsConstructor
@Component
public class RestoreOfflineDataOnNewDeviceCommandHandler
    implements VoidCommandHandler<RestoreOfflineDataOnNewDeviceCommand> {

  @Override
  public void handle(RestoreOfflineDataOnNewDeviceCommand command) {
    // TODO: restore data
    throw new UnsupportedOperationException(
        "RestoreOfflineDataOnNewDeviceCommandHandler not implemented yet");
  }
}
