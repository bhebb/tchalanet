package com.tchalanet.server.core.tenanttheme.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.types.id.TenantId;

/**
 * Query to resolve tenant theme.
 * Maps to spec requirement T6.
 */
public record ResolveTenantThemeQuery(TenantId tenantId)
    implements Query<TenantThemeView> {
  public ResolveTenantThemeQuery {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
  }
}
