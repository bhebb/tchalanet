package com.tchalanet.server.core.tenantgame.infra.persistence;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenantgame.application.port.TenantGamePersistencePort;
import com.tchalanet.server.core.tenantgame.domain.TenantGame;
import com.tchalanet.server.core.tenantgame.infra.persistence.mapper.TenantGameMapper;
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
public class TenantGamePersistenceAdapter implements TenantGamePersistencePort {

  private final TenantGameRepository repository;
  private final GameCatalog gameCatalog;
  private final TenantGameMapper mapper;

  @Override
  public TenantGame save(TenantGame tenantGame) {
    TenantGameJpaEntity entity;

    // Check if update or create
    var existing = repository.findByTenantIdAndGameCode(tenantGame.tenantId().value(), tenantGame.code());

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

  @Override
  public Optional<TenantGame> findByTenantIdAndGameCode(TenantId tenantId, String gameCode) {
    return repository.findByTenantIdAndGameCode(tenantId.value(), gameCode)
        .map(mapper::toDomain);
  }

  @Override
  public List<TenantGame> findAllByTenantId(TenantId tenantId) {
    return repository.findByTenantId(tenantId.value()).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
