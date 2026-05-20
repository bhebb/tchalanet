package com.tchalanet.server.platform.tenantconfig.api.model.request;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Command to update tenant identity fields (name, timezone, currency).
 */
public record UpdateTenantIdentityRequest(
    TenantId tenantId,
    String name,
    String timezone,
    String currency
) {
  public UpdateTenantIdentityRequest {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
