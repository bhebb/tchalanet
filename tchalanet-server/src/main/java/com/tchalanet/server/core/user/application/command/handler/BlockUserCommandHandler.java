package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.app.VoidCommandHandler;
import com.tchalanet.server.core.external.ports.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.BlockUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class BlockUserCommandHandler implements VoidCommandHandler<BlockUserCommand> {

  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakAdapter;

  @Override
  public void handle(BlockUserCommand command) {
    // 1. set status in local DB
    userWriterPort.updateStatus(command.userId(), UserStatus.BLOCKED);
    // 2. block in keycloak
    keycloakAdapter.disableUser(command.keycloakId(), command.reason());
    // 3. close sessions, etc. (could delegate to other ports)
  }
}

