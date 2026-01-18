package com.tchalanet.server.catalog.game.internal.infra.adapter;

import com.tchalanet.server.common.types.id.GameId;
import com.tchalanet.server.catalog.game.application.command.model.UpdateTenantGameCommand;
import com.tchalanet.server.catalog.game.application.port.out.TenantGameWritePort;
import com.tchalanet.server.catalog.game.internal.infra.persistence.TenantGameJpaEntity;
import com.tchalanet.server.catalog.game.internal.infra.persistence.TenantGameRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TenantGameWriteJpaAdapter implements TenantGameWritePort {

  private final TenantGameRepository repo;

  @Override
  public boolean updateByGameId(GameId gameId, UpdateTenantGameCommand cmd) {
    Optional<TenantGameJpaEntity> opt = repo.findByGameId(gameId == null ? null : gameId.uuid());
    if (opt.isEmpty()) return false;
    TenantGameJpaEntity entity = opt.get();

    if (cmd.enabled() != null) entity.setEnabled(cmd.enabled());
    if (cmd.displayName() != null)
      entity.setDisplayName(
          cmd.displayName().trim().substring(0, Math.min(128, cmd.displayName().trim().length())));
    if (cmd.minStake() != null) entity.setMinStake(cmd.minStake());
    if (cmd.maxStake() != null) entity.setMaxStake(cmd.maxStake());
    if (cmd.flags() != null) {
      entity.setFlags(cmd.flags());
    }

    repo.save(entity);
    return true;
  }
}
