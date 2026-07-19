package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.platform.tenantgame.api.error.TenantGameErrorCodes;
import com.tchalanet.server.platform.tenantgame.api.model.request.EnsureTenantGamesRequest;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantGameProvisioningService {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistenceAdapter persistence;
  private final TenantGameBetOptionConfigs betOptionConfigs;

  @Transactional
  public void ensureTenantGame(EnsureTenantGamesRequest request) {
    var game =
        gameCatalog
            .findByCode(request.getGameCode().toUpperCase())
            .orElseThrow(() -> ProblemRest.of(TenantGameErrorCodes.CATALOG_GAME_NOT_FOUND));

    if (persistence.findByTenantIdAndGameCode(request.getTenantId(), game.code()).isPresent()) {
      return;
    }

    persistence.save(
        new TenantGame(
            null,
            request.getTenantId(),
            game.id(),
            game.code().toUpperCase(),
            true,
            true,
            null,
            0,
            null,
            null,
            false,
            null,
            null,
            null,
            betOptionConfigs.defaultsFor(game.code())));
  }
}
