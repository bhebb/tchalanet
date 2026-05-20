package com.tchalanet.server.platform.accesscontrol.api.permissionevaluator;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record CheckPermissionsResult(boolean allowed, Set<String> missingPermissions) {
  public CheckPermissionsResult {
    Objects.requireNonNull(missingPermissions, "missingPermissions cannot be null");
    missingPermissions = Collections.unmodifiableSet(missingPermissions);
  }
}
