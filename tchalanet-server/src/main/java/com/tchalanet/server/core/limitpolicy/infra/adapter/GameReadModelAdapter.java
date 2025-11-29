package com.tchalanet.server.core.limitpolicy.infra.adapter;

import com.tchalanet.server.core.limitpolicy.domain.ports.out.GameReadModelPort;
import com.tchalanet.server.core.tenant.infra.persistence.GameJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameReadModelAdapter implements GameReadModelPort {

  private final GameJpaRepository gameJpaRepository; // Assuming this exists

  @Override
  public Optional<GameInfo> findGameInfoByCode(String gameCode) {
    return gameJpaRepository
        .findByCode(gameCode)
        .map(
            gameEntity ->
                new GameInfo(gameEntity.getId(), gameEntity.getCode(), gameEntity.getName()));
  }
}
