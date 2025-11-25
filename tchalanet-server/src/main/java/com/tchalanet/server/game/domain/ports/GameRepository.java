package com.tchalanet.server.game.domain.ports;

import com.tchalanet.server.game.domain.model.Game;
import java.util.List;
import java.util.Optional;

public interface GameRepository {
  Game save(Game g);

  Optional<Game> findByCode(String code);

  List<Game> findAllActive();

  void softDeleteByCode(String code);
}
