package com.tchalanet.server.platform.tenanttheme.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Query to resolve tenant theme.
 * Maps to spec requirement T6.
 */
public record ResolveTenantThemeQuery(TenantId tenantId) {
  public ResolveTenantThemeQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
