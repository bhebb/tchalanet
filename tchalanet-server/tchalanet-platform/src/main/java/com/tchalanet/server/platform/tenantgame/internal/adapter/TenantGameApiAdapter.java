package com.tchalanet.server.platform.tenantgame.internal.adapter;

import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.types.id.TenantId;
import java.util.List;
import com.tchalanet.server.platform.tenantgame.api.TenantGameApi;
import com.tchalanet.server.platform.tenantgame.api.model.DisableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.EnableTenantGameResult;
import com.tchalanet.server.platform.tenantgame.api.model.request.DisableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnableTenantGameRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.api.model.request.UpdateTenantGameSettingsRequest;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRefView;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameAdminService;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGameProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantGameApiAdapter implements TenantGameApi {

    private final TenantGameAdminService adminService;
    private final TenantGameProvisioningService provisioningService;
    private final TenantGamePersistenceAdapter persistence;

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

    @Override
    public Optional<TenantGameRefView> findByTenantGameId(TenantId tenantId, TenantGameId tenantGameId) {
        return persistence.findByTenantGameId(tenantId, tenantGameId)
            .map(this::toRefView);
    }

    @Override
    public List<TenantGameRefView> listGames(TenantId tenantId) {
        return persistence.findAllByTenantId(tenantId).stream()
            .map(this::toRefView)
            .toList();
    }

    private TenantGameRefView toRefView(com.tchalanet.server.platform.tenantgame.internal.service.TenantGame tg) {
        return new TenantGameRefView(
            tg.tenantGameId(), tg.gameId(), tg.gameCode(),
            tg.enabled(), tg.visibleInPos(), tg.displayName(), tg.displayOrder(),
            tg.minStake(), tg.maxStake());
    }
}
