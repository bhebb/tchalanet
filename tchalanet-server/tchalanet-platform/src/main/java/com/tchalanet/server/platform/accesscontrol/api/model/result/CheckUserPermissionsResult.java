package com.tchalanet.server.platform.accesscontrol.api.model.result;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record CheckUserPermissionsResult(boolean allowed, Set<String> missingPermissions) {

  public CheckUserPermissionsResult {
    Objects.requireNonNull(missingPermissions, "missingPermissions cannot be null");
    missingPermissions = Collections.unmodifiableSet(missingPermissions);
  }
}


