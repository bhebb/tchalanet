package com.tchalanet.server.catalog.game.internal.cache;

/**
 * Cache names for catalog/game.
 * Maps to spec requirement G5 (cache policy).
 */
public final class GameCacheNames {

  private GameCacheNames() {}

  public static final String ACTIVE_GAMES = "catalog:game:active_games";
  public static final String GAME_BY_CODE = "catalog:game:game_by_code";
  public static final String GAME_BY_ID = "catalog:game:game_by_id";
}
