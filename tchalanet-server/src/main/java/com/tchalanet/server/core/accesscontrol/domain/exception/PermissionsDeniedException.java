package com.tchalanet.server.core.accesscontrol.domain.exception;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PermissionsDeniedException extends RuntimeException {

  private final UUID tenantId;
  private final UUID userId;
  private final Set<String> missingPermissions;

  public PermissionsDeniedException(UUID tenantId, UUID userId, Set<String> missingPermissions) {
    super(
        "Missing permissions "
            + missingPermissions
            + " for user "
            + userId
            + " in tenant "
            + tenantId);
    this.tenantId = Objects.requireNonNull(tenantId);
    this.userId = Objects.requireNonNull(userId);
    this.missingPermissions =
        Collections.unmodifiableSet(Objects.requireNonNull(missingPermissions));
  }
}
