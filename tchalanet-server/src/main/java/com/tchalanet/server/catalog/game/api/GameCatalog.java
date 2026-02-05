package com.tchalanet.server.catalog.game.api;

import java.util.List;
import java.util.Optional;

/** Public contract for accessing Game catalog (read-only). */
public interface GameCatalog {

  List<GameView> listAll();

  List<GameView> listActive();

  Optional<GameView> findByCode(String code);

  GameStatsView stats();

  List<GameSummaryView> listRecent(int limit);
}
