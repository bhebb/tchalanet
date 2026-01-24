package com.tchalanet.server.catalog.game.api;

import com.tchalanet.server.common.types.id.GameId;
import java.util.List;
import java.util.Optional;

/**
 * Public API for Game Catalog (read-only).
 * Maps to spec requirement G1 (read operations).
 *
 * This is the ONLY interface that core/tenantgame should depend on.
 * No access to internal persistence or JPA entities allowed.
 */
public interface GameCatalog {

  /**
   * List all active games (deleted_at IS NULL AND active=true).
   * Cacheable.
   * Maps to spec G1 scenario "listActive filters soft-deleted and inactive".
   */
  List<GameView> listActive();

  /**
   * Find game by code (functional key).
   * Returns game even if active=false (but filters deleted_at IS NULL).
   * Cacheable.
   * Maps to spec G1 scenario "findByCode returns inactive game".
   */
  Optional<GameView> findByCode(String code);

  /**
   * Find game by ID (technical key).
   * Filters deleted_at IS NULL.
   * Cacheable.
   * Maps to spec G1 (findById).
   */
  Optional<GameView> findById(GameId id);
}
