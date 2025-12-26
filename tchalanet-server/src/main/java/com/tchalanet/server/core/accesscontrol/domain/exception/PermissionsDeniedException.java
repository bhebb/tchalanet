package com.tchalanet.server.core.accesscontrol.domain.exception;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;

@Getter
public class PermissionsDeniedException extends RuntimeException {

  private final TenantId tenantId;
  private final UserId userId;
  private final Set<String> missingPermissions;

  public PermissionsDeniedException(TenantId tenantId, UserId userId, Set<String> missingPermissions) {
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
