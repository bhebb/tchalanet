package com.tchalanet.server.platform.tenantgame.internal.service;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.api.model.view.TenantGameCatalogItemView;
import com.tchalanet.server.platform.tenantgame.internal.cache.TenantGameCacheNames;
import com.tchalanet.server.platform.tenantgame.internal.persistence.TenantGamePersistenceAdapter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantGameCatalogProjectionService {

  private final GameCatalog gameCatalog;
  private final TenantGamePersistenceAdapter persistence;

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = TenantGameCacheNames.PROJECTION,
      key = "#tenantId.value()",
      condition = "#tenantId != null")
  public List<TenantGameCatalogItemView> getCatalogProjection(TenantId tenantId) {
    var tenantGames = persistence.findAllByTenantId(tenantId);
    var enabledCodes =
        tenantGames.stream()
            .filter(TenantGame::enabled)
            .map(TenantGame::gameCode)
            .collect(java.util.stream.Collectors.toSet());
    return gameCatalog.listActive().stream()
        .map(
            g -> {
              boolean enabledForTenant = enabledCodes.contains(g.code().toUpperCase());
              return new TenantGameCatalogItemView(
                  g.code(),
                  g.name(),
                  g.category(),
                  g.active(),
                  enabledForTenant,
                  g.active(),
                  !g.active() ? "Game is not active in catalog" : null);
            })
        .toList();
  }
}
