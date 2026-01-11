package com.tchalanet.server.core.game.internal.application.port.out;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.List;
import java.util.Optional;

public interface ListTenantGamesPort {
  List<TenantGame> listAll();

  List<TenantGame> listEnabled();

  Optional<TenantGame> findByGameCode(String code);

  Optional<TenantGame> findByGameId(GameId gameId);
}
