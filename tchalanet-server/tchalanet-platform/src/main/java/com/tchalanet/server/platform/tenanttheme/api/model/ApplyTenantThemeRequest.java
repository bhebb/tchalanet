package com.tchalanet.server.platform.tenanttheme.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Command to apply a Theme Preset to a tenant.
 * Maps to spec requirement T1.
 */
public record ApplyTenantThemeRequest(
    TenantId tenantId,
    String presetCode
) {
  public ApplyTenantThemeRequest {
    if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
    if (presetCode == null || presetCode.isBlank()) {
      throw new IllegalArgumentException("presetCode is required");
    }
  }
}
