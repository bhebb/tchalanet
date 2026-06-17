package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.time.TimeProvider;
import com.tchalanet.server.common.types.id.KeycloakUserSub;
import com.tchalanet.server.platform.identity.api.model.request.BootstrapCurrentUserRequest;
import com.tchalanet.server.platform.identity.api.model.result.BootstrapUserResult;
import com.tchalanet.server.platform.identity.internal.model.AppUser;
import com.tchalanet.server.platform.identity.internal.model.UserPreference;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.UserPreferenceJpaAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBootstrapService {

  private final AppUserJpaAdapter users;
  private final UserPreferenceJpaAdapter preferences;
  private final TimeProvider timeProvider;

  @Transactional
  public BootstrapUserResult bootstrap(BootstrapCurrentUserRequest request) {
    var legacyKeycloakSub = KeycloakUserSub.parse(request.externalSubject());
    var existing = users.findByKeycloakSub(legacyKeycloakSub);
    if (existing.isPresent()) {
      var user =
          existing
              .get()
              .syncProfile(
                  request.username(),
                  request.email(),
                  request.phone(),
                  request.firstName(),
                  request.lastName(),
                  request.displayName(),
                  null)
              .touchLogin(timeProvider.nowInstant());
      var saved = users.save(user);
      preferences
          .findByUserId(saved.id())
          .orElseGet(
              () ->
                  preferences.upsert(
                      UserPreference.forUser(saved.id())
                          .applyOverrides(null, null, request.locale(), request.timeZone(), null)));
      return new BootstrapUserResult(false, saved.id());
    }

    var now = timeProvider.nowInstant();
    var saved =
        users.save(
            AppUser.createNew(
                null,
                legacyKeycloakSub,
                request.username(),
                request.email(),
                request.phone(),
                request.firstName(),
                request.lastName(),
                request.displayName(),
                null,
                now));
    preferences.upsert(
        UserPreference.forUser(saved.id())
            .applyOverrides(null, null, request.locale(), request.timeZone(), null));
    return new BootstrapUserResult(true, saved.id());
  }
}
