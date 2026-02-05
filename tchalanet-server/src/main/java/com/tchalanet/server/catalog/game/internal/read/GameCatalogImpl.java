package com.tchalanet.server.catalog.game.internal.read;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.GameStatsView;
import com.tchalanet.server.catalog.game.api.GameSummaryView;
import com.tchalanet.server.catalog.game.api.GameView;
import com.tchalanet.server.catalog.game.internal.cache.GameCacheNames;
import com.tchalanet.server.catalog.game.internal.infra.persistence.GameJpaRepository;
import com.tchalanet.server.catalog.game.internal.mapper.GameMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameCatalogImpl implements GameCatalog {

  private final GameJpaRepository repo;
  private final GameMapper mapper;

  @Override
  @Cacheable(cacheNames = GameCacheNames.ALL)
  public List<GameView> listAll() {
    var entities = repo.findAllByOrderBySortOrderAsc();
    return mapper.toViews(entities);
  }

  @Override
  @Cacheable(cacheNames = GameCacheNames.ACTIVE)
  public List<GameView> listActive() {
    var entities = repo.findByActiveTrueOrderBySortOrder();
    return mapper.toViews(entities);
  }

  @Override
  @Cacheable(cacheNames = GameCacheNames.BY_CODE, key = "#code == null ? '' : #code.toLowerCase()")
  public Optional<GameView> findByCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return repo.findByCode(code).map(mapper::toView);
  }

  @Override
  public GameStatsView stats() {
    long total = repo.countByDeletedAtIsNull();
    long active = repo.countByActiveTrueAndDeletedAtIsNull();
    return new GameStatsView((int) total, (int) active);
  }

  @Override
  public List<GameSummaryView> listRecent(int limit) {
    if (limit <= 0) {
      limit = 10;
    }
    var entities = repo.findTop10ByOrderByUpdatedAtDesc();
    var summaries = mapper.toSummaryViews(entities);
    return summaries.stream().limit(limit).toList();
  }
}
