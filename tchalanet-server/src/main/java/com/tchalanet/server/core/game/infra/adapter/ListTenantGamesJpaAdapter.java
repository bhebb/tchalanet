package com.tchalanet.server.core.game.infra.adapter;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.core.game.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import com.tchalanet.server.core.game.infra.persistence.TenantGameReadRepository;
import com.tchalanet.server.core.game.infra.read.TenantGameView;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ListTenantGamesJpaAdapter implements ListTenantGamesPort {

  private final TenantGameReadRepository repo;

  @Override
  public List<TenantGame> listAll() {
    return repo.listAllForCurrentTenant().stream().map(this::toModel).collect(Collectors.toList());
  }

  @Override
  public List<TenantGame> listEnabled() {
    return repo.listEnabledForCurrentTenant().stream()
        .map(this::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<TenantGame> findByGameCode(String code) {
    return repo.findByGameCode(code).map(this::toModel);
  }

  @Override
  public Optional<TenantGame> findByGameId(UUID gameId) {
    return repo.findByGameId(gameId).map(this::toModel);
  }

  private TenantGame toModel(TenantGameView v) {
    return new TenantGame(
        TenantGameId.of(v.tenantGameId()),
        GameId.of(v.gameId()),
        v.code(),
        v.name(),
        v.category(),
        v.minDigits(),
        v.maxDigits(),
        v.combination(),
        v.enabled(),
        v.displayName(),
        v.minStake(),
        v.maxStake(),
        v.flags());
  }
}
