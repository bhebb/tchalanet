package com.tchalanet.server.platform.tenanttheme.api;

import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.ResolveTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeView;

public interface TenantThemeApi {

    void applyTenantTheme(ApplyTenantThemeRequest request);
    TenantThemeView resolveTenantTheme(ResolveTenantThemeRequest request);
    void deactivateTenantTheme(DeactivateTenantThemeRequest request);
}
