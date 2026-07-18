package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.error.IdentityErrorCodes;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PublicLoginIdentifierService {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,40}$");

  private final AppUserJpaAdapter users;

  public String resolveIdentifier(String identifier) {
    var normalized = normalizeUsername(identifier);
    var user =
        users
            .findByNormalizedUsername(normalized)
            .orElseThrow(PublicLoginIdentifierService::genericFailure);
    if (user.status() != UserStatus.ACTIVE || isBlank(user.email())) {
      throw genericFailure();
    }
    if (users.findExternalSubject(user.id(), IdentityProviderType.FIREBASE).isEmpty()) {
      throw genericFailure();
    }
    return user.email().trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeUsername(String identifier) {
    if (identifier == null) {
      throw genericFailure();
    }
    var normalized = identifier.trim().toLowerCase(Locale.ROOT);
    if (!USERNAME_PATTERN.matcher(normalized).matches()) {
      throw genericFailure();
    }
    return normalized;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static RuntimeException genericFailure() {
    return ProblemRest.of(IdentityErrorCodes.AUTH_INVALID_CREDENTIALS);
  }
}
