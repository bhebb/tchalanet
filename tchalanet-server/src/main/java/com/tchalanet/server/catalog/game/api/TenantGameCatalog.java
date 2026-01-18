package com.tchalanet.server.catalog.game.api;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.catalog.game.domain.model.TenantGame;
import com.tchalanet.server.catalog.game.internal.application.port.out.ListTenantGamesPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** Façade publique pour consulter les jeux (tenant-scoped metadata). */
@Component
@RequiredArgsConstructor
public class TenantGameCatalog {

  private final ListTenantGamesPort reader;

  @Cacheable(cacheNames = "game.all", key = "'v1'")
  public List<TenantGame> listAll() {
    return reader.listAll();
  }

  @Cacheable(cacheNames = "game.enabled", key = "'v1'")
  public List<TenantGame> listEnabled() {
    return reader.listEnabled();
  }

  @Cacheable(cacheNames = "game.byCode", key = "#code == null ? '' : #code.toUpperCase()")
  public Optional<TenantGame> findByGameCode(String code) {
    if (code == null || code.isBlank()) return Optional.empty();
    return reader.findByGameCode(code.trim().toUpperCase());
  }

  @Cacheable(cacheNames = "game.byId", key = "#gameId == null ? '' : #gameId.toString()")
  public Optional<TenantGame> findByGameId(GameId gameId) {
    if (gameId == null) return Optional.empty();
    return reader.findByGameId(gameId);
  }
}
