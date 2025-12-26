package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.ApproveUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApproveUserCommandHandler implements CommandHandler<ApproveUserCommand, Void> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakUserProvisioningPort;

  @Override
  public Void handle(ApproveUserCommand command) {
    var now = Instant.now(); // TODO: injecter une Clock commune si disponible

    var user =
        userReaderPort
            .findById(command.userId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

    var approved = user.approve(now, command.approvedBy());
    var saved = userWriterPort.save(approved);

    if (saved.getKeycloakId() != null) {
      keycloakUserProvisioningPort.updateUserProfile(
          saved.getKeycloakId(),
          saved.getFirstName(),
          saved.getLastName(),
          saved.getEmail(),
          saved.getLocale());
    }

    return null;
  }
}
