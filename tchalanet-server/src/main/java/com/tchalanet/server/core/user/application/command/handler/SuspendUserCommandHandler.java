package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.SuspendUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class SuspendUserCommandHandler implements VoidCommandHandler<SuspendUserCommand> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakUserProvisioningPort;

  @Override
  public void handle(SuspendUserCommand command) {
    var user =
        userReaderPort
            .findById(command.userId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

    var suspended = user.suspend();
    var saved = userWriterPort.save(suspended);

    if (saved.getKeycloakSub() != null) {
      keycloakUserProvisioningPort.disableUser(saved.getKeycloakSub(), command.reason());
    }
  }
}
