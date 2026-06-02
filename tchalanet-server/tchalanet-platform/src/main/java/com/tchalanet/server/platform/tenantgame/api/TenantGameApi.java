package com.tchalanet.server.platform.tenantgame.api;

import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;

public interface TenantGameApi {

    EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request);
    DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request);
    void updateTenantGameSettings(UpdateTenantGameSettingsRequest request);
    void ensureTenantGame(EnsureTenantGamesRequest request);
}
