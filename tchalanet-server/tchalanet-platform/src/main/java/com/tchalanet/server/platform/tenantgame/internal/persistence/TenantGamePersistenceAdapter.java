package com.tchalanet.server.platform.tenantgame.internal.persistence;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.platform.tenantgame.internal.mapper.TenantGameMapper;
import com.tchalanet.server.platform.tenantgame.internal.service.TenantGame;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Adapter for TenantGame persistence.
 * Uses GameCatalog (public API) for game validation, NOT GameJpaRepository (internal).
 * Per inter_domain_calls.md: core/tenantgame depends only on catalog/game/api.
 * Stores game_id as UUID in tenant_game table (no FK relation to game entity).
 */
@Component
@RequiredArgsConstructor
public class TenantGamePersistenceAdapter {

  private final TenantGameJpaRepository repository;
  private final GameCatalog gameCatalog;
  private final TenantGameMapper mapper;

  public TenantGame save(TenantGame tenantGame) {
    TenantGameJpaEntity entity;

    // Check if update or create
    var existing = repository.findByTenantIdAndGameId(tenantGame.tenantId().value(), tenantGame.gameId().value());

    if (existing.isPresent()) {
      entity = existing.get();
      mapper.updateEntityFromDomain(tenantGame, entity);
    } else {
      entity = mapper.toEntity(tenantGame);
      // Validate game exists via GameCatalog (public API only, spec TG5 boundary)
      var gameView = gameCatalog.findByCode(tenantGame.code())
          .orElseThrow(() -> new IllegalStateException("Game code not found in catalog: " + tenantGame.code()));
      // Set gameId directly (UUID)
      entity.setGameId(gameView.id().value());
      // tenantId is set via BaseTenantEntity (inherited from SecurityContext)
    }

    var saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  public Optional<TenantGame> findByTenantIdAndGameCode(TenantId tenantId, String gameCode) {
      var game = gameCatalog.findByCode(gameCode)
          .orElseThrow(() -> new IllegalStateException("Game code not found in catalog: " + gameCode));

    return repository.findByTenantIdAndGameId(tenantId.value(), game.id().value())
        .map(mapper::toDomain);
  }

  public List<TenantGame> findAllByTenantId(TenantId tenantId) {
    return repository.findByTenantId(tenantId.value()).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
