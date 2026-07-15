package com.tchalanet.server.platform.identity.internal.service;

import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.identity.api.IdentityProviderType;
import com.tchalanet.server.platform.identity.api.model.UserStatus;
import com.tchalanet.server.platform.identity.internal.persistence.adapter.AppUserJpaAdapter;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class PublicLoginIdentifierService {

  private static final String GENERIC_FAILURE = "auth.invalid_credentials";
  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,40}$");

  private final AppUserJpaAdapter users;

  public String resolveIdentifier(String identifier) {
    var normalized = normalizeUsername(identifier);
    var user = users.findByNormalizedUsername(normalized).orElseThrow(this::genericFailure);
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
      throw ProblemRest.badRequest(GENERIC_FAILURE);
    }
    var normalized = identifier.trim().toLowerCase(Locale.ROOT);
    if (!USERNAME_PATTERN.matcher(normalized).matches()) {
      throw ProblemRest.badRequest(GENERIC_FAILURE);
    }
    return normalized;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private RuntimeException genericFailure() {
    return ProblemRest.forbidden(GENERIC_FAILURE);
  }
}
