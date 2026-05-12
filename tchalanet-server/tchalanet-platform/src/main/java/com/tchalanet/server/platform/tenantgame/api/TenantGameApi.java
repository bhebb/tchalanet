package com.tchalanet.server.platform.tenantgame.api;

import java.util.List;

import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameCommand;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameCommandResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameCommand;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameCommandResult;
import com.tchalanet.server.platform.tenantgame.api.model.ResolveTenantGamesQuery;
import com.tchalanet.server.platform.tenantgame.api.model.UpdateTenantGamePolicyCommand;

public interface TenantGameApi {

    EnableTenantGameCommandResult enableTenantGame(EnableTenantGameCommand request);
    DisableTenantGameCommandResult disableTenantGame(DisableTenantGameCommand request);
    List<Object> resolveTenantGames(ResolveTenantGamesQuery request);
    void updateTenantGamePolicy(UpdateTenantGamePolicyCommand request);
}
