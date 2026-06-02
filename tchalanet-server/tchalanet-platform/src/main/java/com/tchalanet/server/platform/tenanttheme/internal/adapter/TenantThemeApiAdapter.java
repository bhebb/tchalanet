package com.tchalanet.server.platform.tenanttheme.internal.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenanttheme.api.TenantThemeApi;
import com.tchalanet.server.platform.tenanttheme.api.model.ApplyTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.DeactivateTenantThemeRequest;
import com.tchalanet.server.platform.tenanttheme.api.model.ThemeRuntimeView;
import com.tchalanet.server.platform.tenanttheme.internal.service.TenantThemeAdminService;
import com.tchalanet.server.platform.tenanttheme.internal.service.TenantThemeRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantThemeApiAdapter implements TenantThemeApi {

    private final TenantThemeAdminService adminService;
    private final TenantThemeRuntimeService runtimeService;

    @Override
    public void applyTenantTheme(ApplyTenantThemeRequest request) {
        adminService.applyPreset(request);
    }

    @Override
    public ThemeRuntimeView resolveTenantThemeRuntime(TenantId tenantId, String mode) {
        return runtimeService.getRuntime(tenantId, mode);
    }

    @Override
    public void deactivateTenantTheme(DeactivateTenantThemeRequest request) {
        adminService.deactivate(request);
    }
}
