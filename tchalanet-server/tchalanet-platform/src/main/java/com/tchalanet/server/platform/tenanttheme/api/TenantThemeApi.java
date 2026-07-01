package com.tchalanet.server.platform.tenanttheme.api;

import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import com.tchalanet.server.platform.tenanttheme.api.model.TenantThemeAdminView;

import java.util.Optional;

public interface TenantThemeApi {

    void applyTenantTheme(ApplyTenantThemeRequest request);
    ThemeRuntimeView resolveTenantThemeRuntime(com.tchalanet.server.common.types.id.TenantId tenantId, String mode);
    Optional<TenantThemeAdminView> findActiveTenantTheme(com.tchalanet.server.common.types.id.TenantId tenantId);
    void deactivateTenantTheme(DeactivateTenantThemeRequest request);
}
