package com.tchalanet.server.platform.tenantconfig.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Command to update tenant identity fields (name, timezone, currency).
 */
public record UpdateTenantIdentityCommand(
    TenantId tenantId,
    String name,
    String timezone,
    String currency
) {
  public UpdateTenantIdentityCommand {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
