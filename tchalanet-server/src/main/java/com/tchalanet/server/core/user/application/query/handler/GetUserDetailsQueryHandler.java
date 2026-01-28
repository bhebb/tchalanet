package com.tchalanet.server.core.user.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.user.application.port.out.UserPreferenceReaderPort;
import com.tchalanet.server.core.user.application.port.out.UserReaderPort;
import com.tchalanet.server.core.user.application.query.model.GetUserDetailsQuery;
import com.tchalanet.server.core.user.application.query.model.UserDetails;
import com.tchalanet.server.core.user.domain.model.AppUser;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserDetailsQueryHandler
    implements QueryHandler<GetUserDetailsQuery, UserDetails> {

  private final UserReaderPort userReaderPort;
  private final UserPreferenceReaderPort userPreferenceReaderPort;

  @Override
  public UserDetails handle(GetUserDetailsQuery query) {
    var userId = query.userId();
    var user =
        userReaderPort
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    var pref = userPreferenceReaderPort.findByUserId(user.getId()).orElse(null);

    return toDetails(user, pref);
  }

  private UserDetails toDetails(AppUser user, Object pref) {
    // pref is UserPreference domain object; cast safely
    com.tchalanet.server.core.user.domain.model.UserPreference p = null;
    if (pref instanceof com.tchalanet.server.core.user.domain.model.UserPreference up) p = up;

    return new UserDetails(
        user.getId(),
        user.getKeycloakSub(),
        null, // tenantId not loaded here
        null, // outletId not loaded here
        user.getUsername(),
        user.getDisplayName(),
        user.getFirstName(),
        user.getLastName(),
        user.getPhone(),
        user.getEmail(),
        user.getStatus().name(),
        user.getDisplayName(),
        null,
        null, // app-user locale removed; use preferenceLocale below
        null,
        user.getLastLoginAt(),
        p == null ? null : p.getThemeMode(),
        p == null ? null : p.getDensity(),
        p == null ? null : p.getLocale(),
        p == null ? null : p.getCurrency());
  }
}
