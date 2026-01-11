package com.tchalanet.server.core.game.internal.infra.adapter;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.common.types.id.TenantGameId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.core.game.domain.model.TenantGame;
import com.tchalanet.server.core.game.internal.application.port.out.ListTenantGamesPort;
import com.tchalanet.server.core.game.internal.infra.persistence.TenantGameRepository;
import com.tchalanet.server.core.game.internal.infra.persistence.read.TenantGameView;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ListTenantGamesJpaAdapter implements ListTenantGamesPort {

  private final TenantGameRepository repo;
  private final JsonUtils jsonUtils;

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
  public Optional<TenantGame> findByGameId(GameId gameId) {
    return repo.findTenantGameViewByGameId(gameId == null ? null : gameId.uuid())
        .map(this::toModel);
  }

  private TenantGame toModel(TenantGameView v) {
    Map<String, Object> flags = Collections.emptyMap();
    if (v.flags() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> tmp = jsonUtils.treeToValue(v.flags(), Map.class);
      flags = tmp;
    }

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
        flags);
  }
}
