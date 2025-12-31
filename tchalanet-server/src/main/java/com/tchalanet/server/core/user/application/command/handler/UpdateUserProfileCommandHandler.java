package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateUserProfileCommandHandler
    implements CommandHandler<UpdateUserProfileCommand, Void> {

  private final UserReaderPort userReader;
  private final UserWriterPort userWriter;
  private final KeycloakUserProvisioningPort keycloakIdentityPort;

  @Override
  @Transactional
  public Void handle(UpdateUserProfileCommand command) {
    AppUser existing =
        userReader
            .findById(command.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    String updatedFirstName = command.firstName().orElse(existing.getFirstName());
    String updatedLastName = command.lastName().orElse(existing.getLastName());
    String updatedEmail = command.email().orElse(existing.getEmail());
    String updatedLocale = command.locale().orElse(existing.getLocale());

    var updated =
        existing.syncProfile(
            existing.getUsername(),
            updatedEmail,
            existing.getPhone(),
            updatedFirstName,
            updatedLastName,
            updatedFirstName + " " + updatedLastName,
            existing.getAvatarUrl(),
            updatedLocale,
            existing.getTimeZone());

    var saved = userWriter.save(updated);

    if (saved.getKeycloakId() != null) {
      keycloakIdentityPort.updateUserProfile(
          saved.getKeycloakId(), updatedFirstName, updatedLastName, updatedEmail, updatedLocale);
    }

    return null;
  }
}
