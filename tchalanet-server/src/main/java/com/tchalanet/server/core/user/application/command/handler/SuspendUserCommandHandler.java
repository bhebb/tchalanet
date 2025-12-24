package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.SuspendUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SuspendUserCommandHandler implements CommandHandler<SuspendUserCommand, Void> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakUserProvisioningPort;

  @Override
  public Void handle(SuspendUserCommand command) {
    var user = userReaderPort
        .findById(command.userId())
        .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

    var suspended = user.suspend();
    var saved = userWriterPort.save(suspended);

    if (saved.getKeycloakId() != null) {
      keycloakUserProvisioningPort.disableUser(saved.getKeycloakId(), command.reason());
    }

    return null;
  }
}

