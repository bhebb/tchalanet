package com.tchalanet.server.core.game.domain.usecase;

import com.tchalanet.server.core.game.domain.model.Game;
import com.tchalanet.server.core.game.domain.ports.GameRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GameCrudUseCaseImpl implements GameCrudUseCase {

  private final GameRepository repo;

  @Override
  @Transactional
  public Game create(Game g) {
    return repo.save(g);
  }

  @Override
  public Optional<Game> get(String code) {
    return repo.findByCode(code);
  }

  @Override
  public List<Game> listActive() {
    return repo.findAllActive();
  }

  @Override
  @Transactional
  public Game update(String code, Game g) {
    var opt = repo.findByCode(code);
    if (opt.isEmpty()) throw new IllegalArgumentException("Game not found: " + code);
    // merge allowed fields
    Game existing = opt.get();
    existing.setName(g.getName());
    existing.setCategory(g.getCategory());
    existing.setMinDigits(g.getMinDigits());
    existing.setMaxDigits(g.getMaxDigits());
    existing.setCombination(g.getCombination());
    existing.setDescription(g.getDescription());
    existing.setActive(g.getActive());
    existing.setSortOrder(g.getSortOrder());
    return repo.save(existing);
  }

  @Override
  @Transactional
  public void delete(String code) {
    repo.softDeleteByCode(code);
  }
}
