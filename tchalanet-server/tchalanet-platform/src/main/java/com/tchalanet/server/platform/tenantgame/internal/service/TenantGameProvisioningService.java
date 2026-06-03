package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;

@Service
@RequiredArgsConstructor
public class TenantGameProvisioningService {

    private final GameCatalog gameCatalog;
    private final TenantGamePersistenceAdapter persistence;

    @Transactional
    public void ensureTenantGame(EnsureTenantGamesRequest request) {
        var game = gameCatalog.findByCode(request.getGameCode().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + request.getGameCode()));

        if (persistence.findByTenantIdAndGameCode(request.getTenantId(), game.code()).isPresent()) {
            return;
        }

        persistence.save(new TenantGame(
            null,
            request.getTenantId(),
            game.id(),
            game.code().toUpperCase(),
            true, true, null, 0, null, null, false, null, null, null));
    }
}
