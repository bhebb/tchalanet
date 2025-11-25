package com.tchalanet.server.game.domain.usecase;

import com.tchalanet.server.game.domain.model.Game;
import java.util.List;
import java.util.Optional;

public interface GameCrudUseCase {
  Game create(Game g);

  Optional<Game> get(String code);

  List<Game> listActive();

  Game update(String code, Game g);

  void delete(String code);
}
