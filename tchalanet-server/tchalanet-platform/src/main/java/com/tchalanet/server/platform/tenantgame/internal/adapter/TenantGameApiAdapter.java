package com.tchalanet.server.platform.tenantgame.internal.adapter;

import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameAdminService;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantGameApiAdapter implements TenantGameApi {

    private final TenantGameAdminService adminService;
    private final TenantGameProvisioningService provisioningService;

    @Override
    public EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request) {
        return adminService.enableGame(request);
    }

    @Override
    public DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request) {
        return adminService.disableGame(request);
    }

    @Override
    public void updateTenantGameSettings(UpdateTenantGameSettingsRequest request) {
        adminService.updateSettings(request);
    }

    @Override
    public void ensureTenantGame(EnsureTenantGamesRequest request) {
        provisioningService.ensureTenantGame(request);
    }
}
