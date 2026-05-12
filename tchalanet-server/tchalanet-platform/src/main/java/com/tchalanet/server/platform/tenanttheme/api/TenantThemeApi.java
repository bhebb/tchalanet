package com.tchalanet.server.platform.tenanttheme.api;

import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeCommand;
import com.tchalanet.server.platform.tenanttheme.api.model.ResolveTenantThemeQuery;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeView;

public interface TenantThemeApi {

    void applyTenantTheme(ApplyTenantThemeCommand request);
    TenantThemeView resolveTenantTheme(ResolveTenantThemeQuery request);
    void deactivateTenantTheme(DeactivateTenantThemeCommand request);
}
