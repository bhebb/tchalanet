package com.tchalanet.server.platform.tenanttheme.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Command to deactivate or reset a tenant theme.
 * Maps to spec requirement T5.
 */
public record DeactivateTenantThemeRequest(
    TenantId tenantId
) {
  public DeactivateTenantThemeRequest {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
