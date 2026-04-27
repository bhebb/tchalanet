package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.UpdateUserProfileCommand;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateUserProfileCommandHandler implements VoidCommandHandler<UpdateUserProfileCommand> {

  private final UserReaderPort userReader;
  private final UserWriterPort userWriter;
  private final KeycloakUserProvisioningPort keycloakIdentityPort;
  private final UserPreferenceReaderPort userPreferenceReader;
  private final UserPreferenceWriterPort userPreferenceWriter;

  @Override
  @Transactional
  public void handle(UpdateUserProfileCommand command) {
    AppUser existing =
        userReader
            .findById(command.userId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    String updatedFirstName = command.firstName().orElse(existing.getFirstName());
    String updatedLastName = command.lastName().orElse(existing.getLastName());
    String updatedEmail = command.email().orElse(existing.getEmail());
    String updatedPhone = command.phone().orElse(existing.getPhone());

    // Determine locale: command.locale() (Optional<Locale>) or existing preference
    java.util.Locale existingPrefLocale = null;
    Optional<UserPreference> prefOpt = userPreferenceReader.findByUserId(command.userId());
    if (prefOpt != null && prefOpt.isPresent()) existingPrefLocale = prefOpt.get().getLocale();

    java.util.Locale updatedLocale = command.locale().orElse(existingPrefLocale);

    var updated =
        existing.syncProfile(
            existing.getUsername(),
            updatedEmail,
            updatedPhone,
            updatedFirstName,
            updatedLastName,
            updatedFirstName + " " + updatedLastName,
            existing.getAvatarUrl());

    var saved = userWriter.save(updated);

    // Upsert preference locale (keep others unchanged) using domain typed UserPreference
    var pref = UserPreference.forUser(saved.getId()).applyOverrides(null, null, updatedLocale, null, null);
    userPreferenceWriter.upsert(pref);

    if (saved.getKeycloakSub() != null) {
      keycloakIdentityPort.updateUserProfile(
          saved.getKeycloakSub(), updatedFirstName, updatedLastName, updatedEmail, updatedLocale == null ? null : updatedLocale.toLanguageTag());
    }
  }
}
