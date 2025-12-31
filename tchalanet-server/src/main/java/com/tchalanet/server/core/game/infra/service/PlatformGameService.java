package com.tchalanet.server.core.game.infra.service;

import com.tchalanet.server.core.game.infra.persistence.GameJpaEntity;
import com.tchalanet.server.core.game.infra.persistence.GameJpaRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformGameService {

  private final GameJpaRepository repo;

  public List<GameJpaEntity> list() {
    return repo.findByActiveTrueOrderBySortOrder();
  }

  public GameJpaEntity getByCode(String code) {
    return repo.findByCode(code)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + code));
  }

  public GameJpaEntity get(UUID id) {
    return repo.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
  }

  @Transactional
  public GameJpaEntity create(GameJpaEntity g) {
    g.setId(null);
    return repo.save(g);
  }

  @Transactional
  public GameJpaEntity update(UUID id, GameJpaEntity g) {
    var existing =
        repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Game not found: " + id));
    existing.setCode(g.getCode());
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

  @Transactional
  public void delete(UUID id) {
    repo.deleteById(id);
  }
}
