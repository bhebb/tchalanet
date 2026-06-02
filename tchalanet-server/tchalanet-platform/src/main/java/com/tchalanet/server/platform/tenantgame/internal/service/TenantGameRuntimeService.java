package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameRuntimeView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;

@Service
@RequiredArgsConstructor
public class TenantGameRuntimeService {

    private final TenantGamePersistenceAdapter persistence;
    private final GameCatalog gameCatalog;

    @Transactional(readOnly = true)
    public List<TenantGameRuntimeView> getRuntimeGames(TenantId tenantId) {
        return persistence.findEnabledByTenantId(tenantId).stream()
            .map(g -> {
                var catalogName = gameCatalog.findByCode(g.gameCode())
                    .map(gv -> gv.name())
                    .orElse(g.gameCode());
                var catalogCategory = gameCatalog.findByCode(g.gameCode())
                    .map(gv -> gv.category())
                    .orElse(null);
                return new TenantGameRuntimeView(
                    g.gameCode(),
                    g.displayName() != null ? g.displayName() : catalogName,
                    catalogCategory,
                    g.enabled(),
                    g.visibleInPos(),
                    g.displayOrder(),
                    g.minStake(),
                    g.maxStake(),
                    g.availabilityEnabled(),
                    g.availabilityDays(),
                    g.startLocalTime() != null ? g.startLocalTime().toString() : null,
                    g.endLocalTime() != null ? g.endLocalTime().toString() : null);
            })
            .toList();
    }
}
