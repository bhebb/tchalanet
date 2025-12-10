package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.app.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.ports.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.DeactivateUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeactivateUserCommandHandler implements CommandHandler<DeactivateUserCommand, AppUser> {

  private final UserReaderPort userReader;
  private final UserWriterPort userWriter;
  private final KeycloakUserProvisioningPort keycloakIdentityPort;

  @Override
  @Transactional
  public AppUser handle(DeactivateUserCommand command) {
    AppUser existing =
        userReader
            .findById(command.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    AppUser disabled =
        new AppUser(
            existing.id(),
            existing.keycloakId(),
            existing.tenantId(),
            existing.username(),
            existing.email(),
            existing.phone(),
            existing.firstName(),
            existing.lastName(),
            existing.displayName(),
            existing.avatarUrl(),
            UserStatus.DISABLED,
            existing.locale(),
            existing.timeZone(),
            existing.lastLoginAt());

    var saved = userWriter.save(disabled);

    if (existing.keycloakId() != null) {
      keycloakIdentityPort.disableUser(existing.keycloakId(), command.reason().orElse(""));
    }
    return saved;
  }
}
