package com.tchalanet.server.platform.tenantgame.api;

import java.util.List;

import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.ResolveTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGamePolicyRequest;

public interface TenantGameApi {

    EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request);
    DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request);
    List<Object> resolveTenantGames(ResolveTenantGamesRequest request);
    void updateTenantGamePolicy(UpdateTenantGamePolicyRequest request);
}
