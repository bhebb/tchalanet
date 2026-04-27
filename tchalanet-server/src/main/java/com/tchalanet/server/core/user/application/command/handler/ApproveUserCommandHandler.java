package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.ApproveUserCommand;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import java.time.Instant;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApproveUserCommandHandler implements CommandHandler<ApproveUserCommand, Void> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final KeycloakUserProvisioningPort keycloakUserProvisioningPort;
  private final TimeProvider time;
  private final UserPreferenceReaderPort userPreferenceReaderPort;

  @Override
  public Void handle(ApproveUserCommand command) {
    var now = time.nowInstant();

    var user =
        userReaderPort
            .findById(command.userId())
            .orElseThrow(() -> new IllegalStateException("User not found: " + command.userId()));

    var approved = user.approve(now, command.approvedBy());
    var saved = userWriterPort.save(approved);

    java.util.Locale prefLocale = null;
    var prefOpt = userPreferenceReaderPort.findByUserId(saved.getId());
    if (prefOpt.isPresent() && prefOpt.get().getLocale() != null) prefLocale = prefOpt.get().getLocale();

    if (saved.getKeycloakSub() != null) {
      keycloakUserProvisioningPort.updateUserProfile(
          saved.getKeycloakSub(), saved.getFirstName(), saved.getLastName(), saved.getEmail(), prefLocale == null ? null : prefLocale.toLanguageTag());
    }

    return null;
  }
}
