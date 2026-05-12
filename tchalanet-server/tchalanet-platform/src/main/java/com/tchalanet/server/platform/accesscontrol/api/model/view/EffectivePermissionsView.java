package com.tchalanet.server.platform.accesscontrol.api.model.view;

import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.UserId;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record EffectivePermissionsView(
    TenantId tenantId, UserId userId, RoleId roleId, Set<String> permissionCodes) {

  public EffectivePermissionsView {
    Objects.requireNonNull(tenantId, "tenantId cannot be null");
    Objects.requireNonNull(userId, "userId cannot be null");
    if (permissionCodes == null) {
      permissionCodes = Set.of();
    } else {
      permissionCodes = Collections.unmodifiableSet(permissionCodes);
    }
  }
}


