package com.tchalanet.server.catalog.game.internal.write;

import com.tchalanet.server.catalog.game.api.model.GameView;
import com.tchalanet.server.catalog.game.internal.cache.GameCacheNames;
import com.tchalanet.server.catalog.game.internal.mapper.GameMapper;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaEntity;
import com.tchalanet.server.catalog.game.internal.persistence.GameJpaRepository;
import com.tchalanet.server.catalog.game.internal.web.model.GameCreateRequest;
import com.tchalanet.server.catalog.game.internal.web.model.GameUpdateRequest;
import com.tchalanet.server.common.types.id.GameId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Admin service for Game CRUD (internal write operations).
 * Maps to spec requirement G2 (admin writes).
 * All writes evict relevant caches.
 */
@Service
@RequiredArgsConstructor
public class GameAdminService {

  private final GameJpaRepository repository;
  private final GameMapper mapper;

  @Transactional
  @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
  public GameView create(GameCreateRequest req) {
    var entity = new GameJpaEntity();
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setCategory(req.category());
    entity.setCombination(req.combination());
    entity.setMinDigits(req.minDigits());
    entity.setMaxDigits(req.maxDigits());
    entity.setDescription(req.description());
    entity.setActive(req.active() != null ? req.active() : true);
    entity.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);

    var saved = repository.save(entity);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
  public GameView update(GameId id, GameUpdateRequest req) {
    var entity = repository.findById(id.value())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));

    if (req.name() != null) entity.setName(req.name());
    if (req.category() != null) entity.setCategory(req.category());
    if (req.combination() != null) entity.setCombination(req.combination());
    if (req.minDigits() != null) entity.setMinDigits(req.minDigits());
    if (req.maxDigits() != null) entity.setMaxDigits(req.maxDigits());
    if (req.description() != null) entity.setDescription(req.description());
    if (req.active() != null) entity.setActive(req.active());
    if (req.sortOrder() != null) entity.setSortOrder(req.sortOrder());

    var saved = repository.save(entity);
    return mapper.toView(saved);
  }

  @Transactional
  @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
  public void deactivate(GameId id) {
    var entity = repository.findById(id.value())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
    entity.setActive(false);
    repository.save(entity);
  }

  @Transactional
  @CacheEvict(cacheNames = {GameCacheNames.ACTIVE_GAMES, GameCacheNames.GAME_BY_CODE, GameCacheNames.GAME_BY_ID}, allEntries = true)
  public void softDelete(GameId id) {
    var entity = repository.findById(id.value())
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
    entity.setDeletedAt(Instant.now());
    entity.setActive(false);
    repository.save(entity);
  }
}
