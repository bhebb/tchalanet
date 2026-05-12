package com.tchalanet.server.catalog.game.internal.read;

import com.tchalanet.server.catalog.game.api.GameCatalog;
import com.tchalanet.server.catalog.game.api.GameView;
import com.tchalanet.server.catalog.game.api.GameStatsView;
import com.tchalanet.server.catalog.game.api.GameSummaryView;
import com.tchalanet.server.catalog.game.internal.cache.GameCacheNames;
import com.tchalanet.server.catalog.game.internal.mapper.GameMapper;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaEntity;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaRepository;
import com.tchalanet.server.common.types.id.GameId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of GameCatalog (read-only, cacheable).
 * Maps to spec requirement G1 (read operations) + cache.
 * Filters deleted_at IS NULL on all reads.
 */
@Service
@RequiredArgsConstructor
public class GameCatalogImpl implements GameCatalog {

  private final GameJpaRepository repository;
  private final GameMapper mapper;

  @Override
  @Cacheable(value = GameCacheNames.ACTIVE_GAMES)
  public List<GameView> listActive() {
    return repository.findByActiveTrueAndDeletedAtIsNull()
        .stream()
        .map(mapper::toView)
        .collect(Collectors.toList());
  }

  @Override
  @Cacheable(value = GameCacheNames.GAME_BY_CODE, key = "#code")
  public Optional<GameView> findByCode(String code) {
    return repository.findByCodeAndDeletedAtIsNull(code)
        .map(mapper::toView);
  }

  @Override
  @Cacheable(value = GameCacheNames.GAME_BY_ID, key = "#id.value()")
  public Optional<GameView> findById(GameId id) {
    return repository.findByIdAndDeletedAtIsNull(id.value())
        .map(mapper::toView);
  }

  @Override
  public GameStatsView stats() {
    long total = repository.countAllLive();
    long active = repository.countActiveLive();
    return new GameStatsView((int) total, (int) active);
  }

  @Override
  public List<GameSummaryView> listRecent(int limit) {
    // repository provides top10; limit clamp
    int use = Math.max(1, Math.min(limit, 50));
    return repository.findTop10ByDeletedAtIsNullOrderByUpdatedAtDesc()
        .stream()
        .limit(use)
        .map(this::toSummary)
        .collect(Collectors.toList());
  }

  private GameSummaryView toSummary(GameJpaEntity e) {
    return new GameSummaryView(
        e.getId().toString(),
        e.getCode(),
        e.getName(),
        e.isActive(),
        e.getSortOrder(),
        e.getUpdatedAt()
    );
  }
}
