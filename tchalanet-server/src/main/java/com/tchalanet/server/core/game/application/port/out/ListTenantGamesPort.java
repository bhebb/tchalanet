package com.tchalanet.server.core.game.application.port.out;

import com.tchalanet.server.core.game.domain.model.TenantGame;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListTenantGamesPort {
  List<TenantGame> listAll();

  List<TenantGame> listEnabled();

  Optional<TenantGame> findByGameCode(String code);

  Optional<TenantGame> findByGameId(UUID gameId);
}
