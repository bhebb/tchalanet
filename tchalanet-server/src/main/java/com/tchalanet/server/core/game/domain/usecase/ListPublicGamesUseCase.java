package com.tchalanet.server.core.game.domain.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Liste les jeux visibles sur la page publique. */
public interface ListPublicGamesUseCase {

  /** Liste les jeux publics. Si tenantId est fourni, peut filtrer/adapter selon le tenant. */
  List<Map<String, Object>> listPublicGames(UUID tenantId);
}
