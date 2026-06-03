package com.tchalanet.server.platform.tenanttheme.api;

import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;

public interface TenantThemeApi {

    void applyTenantTheme(ApplyTenantThemeRequest request);
    ThemeRuntimeView resolveTenantThemeRuntime(com.tchalanet.server.common.types.id.TenantId tenantId, String mode);
    void deactivateTenantTheme(DeactivateTenantThemeRequest request);
}
