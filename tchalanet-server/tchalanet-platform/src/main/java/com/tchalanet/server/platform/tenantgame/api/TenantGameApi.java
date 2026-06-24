package com.tchalanet.server.platform.tenantgame.api;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;

import java.util.List;
import java.util.Optional;

public interface TenantGameApi {

    EnableTenantGameResult enableTenantGame(EnableTenantGameRequest request);
    DisableTenantGameResult disableTenantGame(DisableTenantGameRequest request);
    void updateTenantGameSettings(UpdateTenantGameSettingsRequest request);
    void ensureTenantGame(EnsureTenantGamesRequest request);
    Optional<TenantGameRefView> findByTenantGameId(TenantId tenantId, TenantGameId tenantGameId);
    List<TenantGameRefView> listGames(TenantId tenantId);
}
