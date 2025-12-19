package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.query.model.GetUserDetailsQuery;
import com.tchalanet.server.core.user.application.query.model.UserProfileQuery;
import com.tchalanet.server.core.user.domain.model.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserDetailsQueryHandler implements QueryHandler<GetUserDetailsQuery, UserProfileQuery> {

  private final UserReaderPort userReaderPort;
  private final UserPreferenceReaderPort userPreferenceReaderPort;

  @Override
  public UserProfileQuery handle(GetUserDetailsQuery query) {
    UUID userId = query.keycloakUserId();
    AppUser user = userReaderPort
        .findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    var profile = toProfile(user);

    userPreferenceReaderPort.findActiveByUserId(userId).ifPresent(pref -> {
      profile.themeMode = pref.getThemeMode();
      profile.density = pref.getDensity();
      profile.preferenceLocale = pref.getLocale() != null ? pref.getLocale().toLanguageTag() : null;
    });

    return profile;
  }

  private UserProfileQuery toProfile(AppUser user) {
    UserProfileQuery p = new UserProfileQuery();
    p.id = user.getId();
    p.keycloakId = user.getKeycloakId();
    p.tenantId = user.getTenantId();
    p.username = user.getUsername();
    p.email = user.getEmail();
    p.firstName = user.getFirstName();
    p.lastName = user.getLastName();
    p.displayName = user.getDisplayName();
    p.locale = user.getLocale();
    p.timeZone = user.getTimeZone();
    p.lastLoginAt = user.getLastLoginAt();
    return p;
  }
}
