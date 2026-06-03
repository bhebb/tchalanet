package com.tchalanet.server.platform.tenanttheme.api.model;

import com.tchalanet.server.common.types.id.TenantId;

/**
 * Request to update mutable tenant theme settings.
 * V1: only defaultMode is mutable. Token/font overrides deferred to V2.
 */
public record UpdateTenantThemeSettingsRequest(
    TenantId tenantId,
    String defaultMode
) {
    public UpdateTenantThemeSettingsRequest {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (defaultMode != null) {
            var upper = defaultMode.toUpperCase();
            if (!upper.equals("LIGHT") && !upper.equals("DARK") && !upper.equals("SYSTEM")) {
                throw new IllegalArgumentException("defaultMode must be LIGHT, DARK, or SYSTEM");
            }
        }
    }
}
