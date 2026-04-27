package com.tchalanet.server.core.user.application.command.handler;

import com.tchalanet.server.common.bus.CommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.core.external.port.out.KeycloakUserProvisioningPort;
import com.tchalanet.server.core.user.application.command.model.CreateUserCommand;
import com.tchalanet.server.core.user.application.command.model.CreateUserResult;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceWriterPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserWriterPort;
import com.tchalanet.server.core.user.domain.model.AppUser;
import com.tchalanet.server.core.user.domain.model.UserPreference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateUserCommandHandler implements CommandHandler<CreateUserCommand, CreateUserResult> {

  private final UserReaderPort userReaderPort;
  private final UserWriterPort userWriterPort;
  private final UserPreferenceWriterPort prefWriterPort;

  private final KeycloakUserProvisioningPort keycloakProvisioningPort;
  private final TimeProvider time;

  @Override
  public CreateUserResult handle(CreateUserCommand command) {
    // pre-check (DB uniqueness remains source of truth)
    if (userReaderPort.findByEmailOrPhone(command.email(), command.phone()).isPresent()) {
      throw new IllegalStateException("User already exists with this email or phone");
    }

    // Keycloak first (keeps DB tx short; consider afterCommit later)
    java.util.Locale kcLocale = command.prefLocale().map(java.util.Locale::forLanguageTag).orElse(null);
    UUID keycloakUuid = provisionInKeycloak(command, kcLocale);
    KeycloakUserSub keycloakSub = KeycloakUserSub.of(keycloakUuid);

    return createInDb(command, keycloakSub);
  }

  @Transactional
  protected CreateUserResult createInDb(CreateUserCommand command, KeycloakUserSub keycloakSub) {
    var now = time.nowInstant();

    AppUser newUser =
        AppUser.createNew(
            null,
            keycloakSub,
            resolveUsername(command.email(), command.phone()),
            command.email(),
            command.phone(),
            command.firstName(),
            command.lastName(),
            buildDisplayName(command.firstName(), command.lastName()),
            null,
            now);

    userWriterPort.save(newUser);

    // Upsert preferences (optional overrides)
    UserPreference pref =
        UserPreference.forUser(newUser.getId())
            .applyOverrides(
                command.prefThemeMode().orElse(null),
                command.prefDensity().orElse(null),
                command.prefLocale().map(java.util.Locale::forLanguageTag).orElse(null),
                command.prefTimeZone().map(java.time.ZoneId::of).orElse(null),
                command.prefCurrency().map(java.util.Currency::getInstance).orElse(null)
            );

    prefWriterPort.upsert(pref);

    return new CreateUserResult(newUser.getId());
  }

  private UUID provisionInKeycloak(CreateUserCommand command, java.util.Locale kcLocale) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("email", command.email());
    payload.put("phoneNumber", command.phone());
    payload.put("firstName", command.firstName());
    payload.put("lastName", command.lastName());
    payload.put("locale", kcLocale == null ? null : kcLocale.toLanguageTag());
    payload.put("sendInvitation", command.sendInvitation());
    payload.put("initialRoles", command.initialRoles());

    var response = keycloakProvisioningPort.createUser(payload);
    Object idValue = response.get("id");
    if (idValue == null) throw new IllegalStateException("Keycloak did not return an id");
    return UUID.fromString(idValue.toString());
  }

  private String resolveUsername(String email, String phone) {
    if (email != null && !email.isBlank()) return email.trim();
    if (phone != null && !phone.isBlank()) return phone.trim();
    throw new IllegalArgumentException("Either email or phone must be provided");
  }

  private String buildDisplayName(String firstName, String lastName) {
    var first = firstName != null ? firstName : "";
    var last = lastName != null ? lastName : "";
    return (first + " " + last).trim();
  }
}
